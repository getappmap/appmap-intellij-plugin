package appland.cli;

import appland.files.AppMapVfsUtils;
import appland.settings.AppMapApplicationSettingsService;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.KillableProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.ex.temp.TempFileSystem;
import com.intellij.util.system.CpuArch;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultCommandLineService implements AppLandCommandLineService {
    private static final Logger LOG = Logger.getInstance(DefaultCommandLineService.class);

    // must be accessed in a synchronized block
    private final Map<VirtualFile, CliProcesses> processes = new HashMap<>();

    @Override
    public synchronized boolean isRunning(@NotNull VirtualFile directory, boolean strict) {
        if (strict) {
            return processes.containsKey(directory);
        }

        return processes.keySet().stream().anyMatch(dir -> VfsUtilCore.isAncestor(dir, directory, false));
    }

    @Override
    public synchronized void start(@NotNull VirtualFile directory) throws ExecutionException {
        if (!isDirectoryEnabled(directory)) {
            return;
        }

        for (var it = processes.entrySet().iterator(); it.hasNext(); ) {
            var dirAndProcesses = it.next();

            // stop early, if there are already processes for exactly the directory.
            if (directory.equals(dirAndProcesses.getKey())) {
                return;
            }

            // stop processes serving for subdirectories of the new directory
            if (VfsUtilCore.isAncestor(directory, dirAndProcesses.getKey(), false)) {
                it.remove();
                stopLocked(dirAndProcesses.getValue());
            }
        }

        // verify that no other process is serving a parent directory
        for (var entry : processes.entrySet()) {
            if (VfsUtilCore.isAncestor(entry.getKey(), directory, true)) {
                LOG.error("Attempted to launch a new service for a directory, which is already being processed");
                return;
            }
        }

        // start new services
        var newProcesses = startProcesses(directory);
        if (newProcesses != null) {
            processes.put(directory, newProcesses);
        }
    }

    /**
     * File appmap.yml is a marker file to tell that CLI processes may be launched for a directory.
     *
     * @return {@code true} if the directory may have CLI processes watching it
     */
    private boolean isDirectoryEnabled(@NotNull VirtualFile directory) {
        return directory.findChild("appmap.yml") != null;
    }

    @Override
    public synchronized void stop(@NotNull VirtualFile directory) {
        var entry = processes.remove(directory);
        if (entry != null) {
            stopLocked(entry);
        }
    }

    @Override
    public synchronized void refreshForOpenProjects() {
        try {
            doRefreshForOpenProjectsLocked();
        } finally {
            var messageBus = ApplicationManager.getApplication().getMessageBus();
            messageBus.syncPublisher(AppLandCommandLineListener.TOPIC).afterRefreshForProjects();
        }
    }

    @Override
    public synchronized @NotNull List<VirtualFile> getActiveRoots() {
        return List.copyOf(processes.keySet());
    }

    @Override
    public synchronized void stopAll() {
        for (var processes : processes.values()) {
            try {
                stopLocked(processes);
            } catch (Exception e) {
                LOG.error("Error shutting down processes on disposal", e);
            }
        }

        processes.clear();
    }

    private void stopLocked(@NotNull CliProcesses value) {
        try {
            shutdownInBackground(value.indexer);
        } catch (Exception e) {
            LOG.warn("Error shutting down indexer", e);
        }

        if (value.scanner != null) {
            try {
                shutdownInBackground(value.scanner);
            } catch (Exception e) {
                LOG.warn("Error shutting down scanner", e);
            }
        }
    }

    @Override
    public void dispose() {
        stopAll();
    }

    private void doRefreshForOpenProjectsLocked() {
        var topLevelRoots = new VfsUtilCore.DistinctVFilesRootsCollection(VirtualFile.EMPTY_ARRAY);
        for (var project : ProjectManager.getInstance().getOpenProjects()) {
            if (!project.isDefault() && !project.isDisposed()) {
                for (var contentRoot : ProjectRootManager.getInstance(project).getContentRoots()) {
                    if (isDirectoryEnabled(contentRoot)) {
                        topLevelRoots.add(contentRoot);
                    }
                }
            }
        }

        // remove processes of roots, which no longer have a matching content root in a project
        for (var activeRoot : List.copyOf(processes.keySet())) {
            if (!topLevelRoots.contains(activeRoot)) {
                try {
                    stop(activeRoot);
                } catch (Exception e) {
                    LOG.warn("Error stopping processes for root: " + activeRoot.getPath());
                }
            }
        }

        // launch missing cli processes
        for (var root : topLevelRoots) {
            try {
                start(root);
            } catch (ExecutionException e) {
                LOG.warn("Error launching cli process for root: " + root);
            }
        }
    }

    private static @Nullable CliProcesses startProcesses(@NotNull VirtualFile directory) throws ExecutionException {
        if (!isSupported()) {
            return null;
        }

        var indexerPath = AppLandDownloadService.getInstance().getDownloadFilePath(CliTool.AppMap);
        if (indexerPath == null) {
            return null;
        }

        var scannerPath = AppLandDownloadService.getInstance().getDownloadFilePath(CliTool.Scanner);
        if (scannerPath == null) {
            return null;
        }

        assert Files.exists(indexerPath);
        assert Files.exists(scannerPath);

        // don't launch for in-memory directories in unit test mode
        if (ApplicationManager.getApplication().isUnitTestMode() && directory.getFileSystem() instanceof TempFileSystem) {
            return null;
        }

        var workingDir = AppMapVfsUtils.asNativePath(directory);

        var indexer = startProcess(workingDir, indexerPath.toString(), "index", "--watch");
        var scanner = AppMapApplicationSettingsService.getInstance().isEnableFindings()
                ? startProcess(workingDir, scannerPath.toString(), "scan", "--watch")
                : null;
        return new CliProcesses(indexer, scanner);
    }

    private static boolean isSupported() {
        return SystemInfo.isMac && (CpuArch.isIntel64() || CpuArch.isArm64())
                || SystemInfo.isLinux && CpuArch.isIntel64()
                || SystemInfo.isWindows && CpuArch.isIntel64();
    }

    private static @NotNull KillableProcessHandler startProcess(@NotNull Path workingDir,
                                                                @NotNull String... commandLine) throws ExecutionException {

        if (!Files.isDirectory(workingDir)) {
            throw new IllegalStateException("Directory does not exist: " + workingDir);
        }

        var command = new GeneralCommandLine(commandLine);
        command.withWorkDirectory(workingDir.toString());
        command.withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.SYSTEM);

        var processHandler = new KillableProcessHandler(command);
        processHandler.addProcessListener(new ProcessAdapter() {
            @Override
            public void processTerminated(@NotNull ProcessEvent event) {
                LOG.warn("CLI tool terminated: " + command + ", exit code: " + event.getExitCode());
            }

            @Override
            public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
                LOG.warn(event.getText());
            }
        });
        processHandler.startNotify();
        return processHandler;
    }

    private static void shutdownInBackground(@NotNull KillableProcessHandler process) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            process.destroyProcess();
            process.waitFor(500);

            if (!process.isProcessTerminated()) {
                process.killProcess();
            }
        });
    }

    private static final class CliProcesses {
        @NotNull KillableProcessHandler indexer;
        // only launched if the enableFindings flags is set
        @Nullable KillableProcessHandler scanner;

        CliProcesses(@NotNull KillableProcessHandler indexer, @Nullable KillableProcessHandler scanner) {
            this.indexer = indexer;
            this.scanner = scanner;
        }
    }
}
