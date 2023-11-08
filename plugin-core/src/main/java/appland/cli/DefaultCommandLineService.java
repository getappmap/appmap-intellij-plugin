package appland.cli;

import appland.config.AppMapConfigFile;
import appland.config.AppMapConfigFileListener;
import appland.files.AppMapVfsUtils;
import com.intellij.execution.CantRunException;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.PtyCommandLine;
import com.intellij.execution.process.*;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.ex.temp.TempFileSystem;
import com.intellij.util.io.BaseOutputReader;
import com.intellij.util.system.CpuArch;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.concurrent.GuardedBy;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultCommandLineService implements AppLandCommandLineService {
    private static final Logger LOG = Logger.getInstance(DefaultCommandLineService.class);

    @GuardedBy("this")
    protected final Map<VirtualFile, CliProcesses> processes = new HashMap<>();

    public DefaultCommandLineService() {
        var connection = ApplicationManager.getApplication().getMessageBus().connect(this);
        connection.subscribe(AppMapConfigFileListener.TOPIC, this::refreshForOpenProjectsInBackground);
    }

    @Override
    public synchronized boolean isRunning(@NotNull VirtualFile directory, boolean strict) {
        if (strict) {
            return processes.containsKey(directory);
        }

        return processes.keySet().stream().anyMatch(dir -> VfsUtilCore.isAncestor(dir, directory, false));
    }

    @Override
    public synchronized void start(@NotNull VirtualFile directory, boolean waitForProcessTermination) throws ExecutionException {
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
                stopLocked(dirAndProcesses.getValue(), waitForProcessTermination);
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
            newProcesses.addProcessListener(LoggingProcessAdapter.INSTANCE, this);
            attachIndexEventsListener(newProcesses.indexer);

            newProcesses.startNotify();
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
    public synchronized void stop(@NotNull VirtualFile directory, boolean waitForTermination) {
        var entry = processes.remove(directory);
        if (entry != null) {
            stopLocked(entry, waitForTermination);
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
    public void stopAll(boolean waitForTermination) {
        List<CliProcesses> activeProcesses;
        synchronized (this) {
            activeProcesses = List.copyOf(processes.values());
            processes.clear();
        }

        for (var processes : activeProcesses) {
            try {
                stopLocked(processes, waitForTermination);
            } catch (Exception e) {
                LOG.warn("Error shutting down processes on disposal", e);
            }
        }
    }

    @Override
    public @Nullable GeneralCommandLine createInstallCommand(@NotNull Path installLocation, @NotNull String language) {
        return createAppMapPtyCommand("install", "-d", installLocation.toString());
    }

    @Override
    public @Nullable GeneralCommandLine createGenerateOpenApiCommand(@NotNull VirtualFile projectRoot) {
        var localPath = projectRoot.getFileSystem().getNioPath(projectRoot);
        if (localPath == null) {
            LOG.debug("Project root is not on the local filesystem", projectRoot);
            return null;
        }

        return createAppMapCommand("openapi", "--appmap-dir", localPath.toString());
    }

    @Override
    public @Nullable GeneralCommandLine createPruneAppMapCommand(@NotNull VirtualFile appMapFile, @NotNull String maxSize) {
        var localPath = appMapFile.getFileSystem().getNioPath(appMapFile);
        if (localPath == null) {
            LOG.debug("AppMap file is not on the local filesystem", appMapFile);
            return null;
        }

        return createAppMapCommand("prune", localPath.toString(), "--size", "10mb", "--output-data", "--auto");
    }

    @Override
    public @Nullable GeneralCommandLine createAppMapStatsCommand(@NotNull VirtualFile appMapFile) {
        var localPath = appMapFile.getFileSystem().getNioPath(appMapFile);
        if (localPath == null) {
            LOG.debug("AppMap file is not on the local filesystem", appMapFile);
            return null;
        }

        return createAppMapCommand("stats", "--appmap-file", localPath.toString(), "--limit", String.valueOf(Long.MAX_VALUE), "--format", "json");
    }

    @Override
    public synchronized String toString() {
        return "DefaultCommandLineService{" +
                "processes=" + processes +
                '}';
    }

    private void stopLocked(@NotNull CliProcesses value, boolean waitForTermination) {
        try {
            shutdownInBackground(value.indexer, waitForTermination);
        } catch (Exception e) {
            LOG.warn("Error shutting down indexer", e);
        }

        try {
            shutdownInBackground(value.scanner, waitForTermination);
        } catch (Exception e) {
            LOG.warn("Error shutting down scanner", e);
        }
    }

    @Override
    public void dispose() {
        stopAll(false);
    }

    /**
     * Listen to index events of the indexer.
     *
     * @param processHandler Indexer process
     */
    private void attachIndexEventsListener(@NotNull ProcessHandler processHandler) {
        processHandler.addProcessListener(new ProcessAdapter() {
            @Override
            public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Indexer output: " + event.getText());
                }

                if (outputType == ProcessOutputType.STDOUT && IndexerEventUtil.isIndexedEvent(event.getText())) {
                    var filePath = IndexerEventUtil.extractIndexedFilePath(event.getText());
                    if (filePath != null) {
                        try {
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("Refreshing local filesystem for indexed file: " + filePath);
                            }

                            // Refresh parent directory of the indexed AppMap, because it contains both
                            // myAppMap.appmap.json file and the corresponding metadata directory myAppMap/
                            requestVirtualFileRefresh(Path.of(filePath).getParent());
                        } catch (InvalidPathException e) {
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("Error parsing indexed file path: " + filePath, e);
                            }
                        }
                    }
                }
            }
        }, this);
    }

    // extracted as method to allow testing it
    protected void requestVirtualFileRefresh(@NotNull Path path) {
        VfsUtil.markDirtyAndRefresh(true, false, false, path.toFile());
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

        var scannerPath = AppLandDownloadService.getInstance().getDownloadFilePath(CliTool.Scanner);
        if (scannerPath == null || Files.notExists(scannerPath)) {
            return null;
        }

        var workingDir = AppMapVfsUtils.asNativePath(directory);
        var watchedDir = findWatchedAppMapDirectory(workingDir);

        // create AppMap directory if it does not exist yet
        if (Files.notExists(watchedDir)) {
            try {
                Files.createDirectories(watchedDir);
            } catch (Exception e) {
                LOG.debug("Failed to create AppMap directory: " + watchedDir, e);
            }
        }

        var indexer = startProcess(workingDir, indexerPath.toString(), "index", "--verbose", "--watch", "--appmap-dir", watchedDir.toString());
        try {
            var scanner = startProcess(workingDir, scannerPath.toString(), "scan", "--watch", "--appmap-dir", watchedDir.toString());
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
        // or which don't match the settings anymore. We need to launch the scanner when "enableFindings" changes.
        // We're iterating on a copy, because stop() is called inside the loop and modifies "processes"
        for (var entry : List.copyOf(processes.entrySet())) {
            var activeRoot = entry.getKey();
            if (!topLevelRoots.contains(activeRoot)) {
                try {
                    stop(activeRoot, false);
                } catch (Exception e) {
                    LOG.warn("Error stopping processes for root: " + activeRoot.getPath());
                }
            }
        }

        // launch missing cli processes
        for (var root : topLevelRoots) {
            try {
                start(root, false);
            } catch (ExecutionException e) {
                LOG.warn("Error launching cli process for root: " + root);
            }
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

        return new KillableProcessHandler(command) {
            @Override
            protected BaseOutputReader.@NotNull Options readerOptions() {
                return BaseOutputReader.Options.BLOCKING;
            }
        };
    }

    private static void shutdownInBackground(@NotNull KillableProcessHandler process, boolean waitForTermination) throws Exception {
        var future = ApplicationManager.getApplication().executeOnPooledThread(() -> {
            process.destroyProcess();
            process.waitFor(500);

            if (!process.isProcessTerminated()) {
                process.killProcess();
            }

            if (waitForTermination) {
                process.waitFor();
            }
        });

        if (waitForTermination) {
            future.get();
        }
    }

    private static @Nullable GeneralCommandLine createAppMapCommand(@NotNull String... parameters) {
        return createAppMapCliCommand(false, parameters);
    }

    private static @Nullable GeneralCommandLine createAppMapPtyCommand(@NotNull String... parameters) {
        return createAppMapCliCommand(true, parameters);
    }

    private static @Nullable GeneralCommandLine createAppMapCliCommand(boolean createPtyCommandLine, @NotNull String... parameters) {
        var toolPath = AppLandDownloadService.getInstance().getDownloadFilePath(CliTool.AppMap);
        if (toolPath == null || Files.notExists(toolPath)) {
            LOG.debug("AppMap CLI executable not found: " + toolPath);
            return null;
        }

        GeneralCommandLine cmd;
        if (createPtyCommandLine) {
            var ptyCmd = new PtyCommandLine();
            ptyCmd.withConsoleMode(true);
            cmd = ptyCmd;
        } else {
            cmd = new GeneralCommandLine();
        }
        cmd.withExePath(toolPath.toString());
        cmd.withParameters(parameters);
        cmd.withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE);

        LOG.debug("AppMap CLI command line " + cmd.getCommandLineString());
        return cmd;
    }

    @ToString
    @EqualsAndHashCode
    @AllArgsConstructor
    protected static final class CliProcesses {
        @NotNull KillableProcessHandler indexer;
        @NotNull KillableProcessHandler scanner;

        private void startNotify() {
            indexer.startNotify();
            scanner.startNotify();
        }

        private void addProcessListener(@NotNull ProcessListener listener, @NotNull Disposable disposable) {
            indexer.addProcessListener(listener, disposable);
            scanner.addProcessListener(listener, disposable);
        }
    }
}
