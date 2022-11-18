package appland.cli;

import appland.config.AppMapConfigFile;
import appland.config.AppMapConfigFileListener;
import appland.files.AppMapVfsUtils;
import appland.settings.AppMapApplicationSettingsService;
import appland.settings.AppMapSettingsListener;
import com.intellij.execution.CantRunException;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.PtyCommandLine;
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
import com.intellij.util.io.BaseOutputReader;
import com.intellij.util.system.CpuArch;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultCommandLineService implements AppLandCommandLineService {
    private static final Logger LOG = Logger.getInstance(DefaultCommandLineService.class);

    // must be accessed in a synchronized block
    private final Map<VirtualFile, CliProcesses> processes = new HashMap<>();

    public DefaultCommandLineService() {
        var connection = ApplicationManager.getApplication().getMessageBus().connect(this);
        connection.subscribe(AppMapConfigFileListener.TOPIC, this::refreshForOpenProjectsInBackground);
        connection.subscribe(AppMapSettingsListener.TOPIC, new AppMapSettingsListener() {
            @Override
            public void enableFindingsChanged() {
                refreshForOpenProjectsInBackground();
            }
        });
    }

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
        assert !ApplicationManager.getApplication().isDispatchThread();

        try {
            doRefreshForOpenProjectsLocked();
        } finally {
            var messageBus = ApplicationManager.getApplication().getMessageBus();
            messageBus.syncPublisher(AppLandCommandLineListener.TOPIC).afterRefreshForProjects();
        }
    }

    @Override
    public void refreshForOpenProjectsInBackground() {
        ApplicationManager.getApplication().executeOnPooledThread(this::refreshForOpenProjects);
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

    @Override
    public @Nullable GeneralCommandLine createInstallCommand(@NotNull Path installLocation, @NotNull String language) {
        var indexerPath = AppLandDownloadService.getInstance().getDownloadFilePath(CliTool.AppMap);
        if (indexerPath == null || Files.notExists(indexerPath)) {
            LOG.debug("CLI executable not found: " + indexerPath);
            return null;
        }

        var cmd = new PtyCommandLine();
        cmd.withExePath(indexerPath.toString());
        cmd.withParameters("install", "-d", installLocation.toString());
        cmd.withConsoleMode(false);
        cmd.withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE);

        LOG.debug("AppMap command line " + cmd.getCommandLineString());
        return cmd;
    }

    @Override
    public @Nullable GeneralCommandLine createGenerateOpenApiCommand(@NotNull VirtualFile projectRoot) {
        var toolPath = AppLandDownloadService.getInstance().getDownloadFilePath(CliTool.AppMap);
        if (toolPath == null || Files.notExists(toolPath)) {
            LOG.debug("CLI executable not found: " + toolPath);
            return null;
        }

        var localPath = projectRoot.getFileSystem().getNioPath(projectRoot);
        if (localPath == null) {
            LOG.debug("Project root is not on the local filesystem", projectRoot);
            return null;
        }

        var cmd = new PtyCommandLine();
        cmd.withExePath(toolPath.toString());
        cmd.withParameters("openapi", "--appmap-dir", localPath.toString());
        cmd.withConsoleMode(false);
        cmd.withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE);

        LOG.debug("AppMap OpenAPI command line " + cmd.getCommandLineString());
        return cmd;
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

        var scannerProcessRequired = AppMapApplicationSettingsService.getInstance().isAnalysisEnabled();

        // remove processes of roots, which no longer have a matching content root in a project
        // or which don't match the settings anymore. We need to launch the scanner when "enableFindings" changes
        for (var entry : processes.entrySet()) {
            var activeRoot = entry.getKey();
            var scannerProcessMismatch = scannerProcessRequired == (entry.getValue().scanner == null);
            if (!topLevelRoots.contains(activeRoot) || scannerProcessMismatch) {
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

        // don't launch for in-memory directories in unit test mode
        if (ApplicationManager.getApplication().isUnitTestMode() && directory.getFileSystem() instanceof TempFileSystem) {
            return null;
        }

        var indexerPath = AppLandDownloadService.getInstance().getDownloadFilePath(CliTool.AppMap);
        if (indexerPath == null || Files.notExists(indexerPath)) {
            return null;
        }

        var launchScanner = AppMapApplicationSettingsService.getInstance().isAnalysisEnabled();
        var scannerPath = AppLandDownloadService.getInstance().getDownloadFilePath(CliTool.Scanner);
        if (launchScanner && (scannerPath == null || Files.notExists(scannerPath))) {
            return null;
        }

        var workingDir = AppMapVfsUtils.asNativePath(directory);
        var watchedDir = findWatchedAppMapDirectory(workingDir).toString();

        var indexer = startProcess(workingDir, indexerPath.toString(), "index", "--watch", "--appmap-dir", watchedDir);
        if (!launchScanner) {
            return new CliProcesses(indexer, null);
        }

        try {
            var scanner = startProcess(workingDir, scannerPath.toString(), "scan", "--watch", "--appmap-dir", watchedDir);
            return new CliProcesses(indexer, scanner);
        } catch (ExecutionException e) {
            LOG.debug("Error executing scanner process. Attempting to terminate indexer process.");
            try {
                indexer.killProcess();
            } catch (Exception ex) {
                LOG.debug("Error terminating scanner process", ex);
            }
            throw new CantRunException("Failed to execute AppMap scanner process", e);
        }
    }

    /**
     * Locate the directory, which should be watched for new .appmap.json files.
     * This is either the directory configured in appmap.yml or the base directory itself.
     */
    private static @NotNull Path findWatchedAppMapDirectory(@NotNull Path baseDirectory) {
        var appMapConfig = AppMapConfigFile.parseConfigFile(baseDirectory.resolve("appmap.yml"));
        if (appMapConfig == null || appMapConfig.getAppMapDir() == null) {
            return baseDirectory;
        }

        try {
            var configuredPath = Paths.get(appMapConfig.getAppMapDir());
            return configuredPath.isAbsolute()
                    ? configuredPath
                    : baseDirectory.resolve(configuredPath);
        } catch (Exception e) {
            return baseDirectory;
        }
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

        var processHandler = new KillableProcessHandler(command) {
            {
                addProcessListener(new ProcessAdapter() {
                    @Override
                    public void processTerminated(@NotNull ProcessEvent event) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("CLI tool terminated: " + command + ", exit code: " + event.getExitCode());
                        }
                    }

                    @Override
                    public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug(event.getText());
                        }
                    }
                });
            }

            @Override
            protected BaseOutputReader.@NotNull Options readerOptions() {
                return BaseOutputReader.Options.forMostlySilentProcess();
            }
        };

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
