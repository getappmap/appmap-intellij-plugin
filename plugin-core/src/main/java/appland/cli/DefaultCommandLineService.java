package appland.cli;

import appland.config.AppMapConfigFile;
import appland.config.AppMapConfigFileListener;
import appland.files.AppMapFiles;
import appland.files.AppMapVfsUtils;
import appland.settings.AppMapApplicationSettingsService;
import appland.settings.AppMapSecureApplicationSettingsService;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.PtyCommandLine;
import com.intellij.execution.process.KillableProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessOutputType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.ex.temp.TempFileSystem;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.io.BaseOutputReader;
import com.intellij.util.net.HttpConfigurable;
import com.intellij.util.system.CpuArch;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.jvnet.winp.WinpException;

import javax.annotation.concurrent.GuardedBy;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.intellij.openapi.util.text.StringUtil.isNotEmpty;

public class DefaultCommandLineService implements AppLandCommandLineService {
    private static final Logger LOG = Logger.getInstance(DefaultCommandLineService.class);
    // initial delay for the first restart attempt
    private static final long INITIAL_RESTART_DELAY_MILLIS = 5_000;
    // factor to calculate the next restart delay based on the previous delay
    private static final double NEXT_RESTART_FACTOR = 1.5;
    // three restarts at most (5_000 * 1.5^3 > 15_000)
    private static final long MAX_RESTART_DELAY_MILLIS = INITIAL_RESTART_DELAY_MILLIS * 3;
    // a value of "0" indicates that process restart is disabled
    private static final Key<Long> NEXT_RESTART_DELAY = Key.create("appmap.processRestartDelay");

    // toString() uses unguarded access, synchronizing it could create deadlocks
    @GuardedBy("this")
    protected final ConcurrentHashMap<VirtualFile, CliProcesses> processes = new ConcurrentHashMap<>();

    public DefaultCommandLineService() {
        var connection = ApplicationManager.getApplication().getMessageBus().connect(this);
        connection.subscribe(AppMapConfigFileListener.TOPIC, (AppMapConfigFileListener) this::refreshForOpenProjectsInBackground);
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
        if (!AppMapFiles.isDirectoryEnabled(directory)) {
            return;
        }

        // stop early, if there are already processes for exactly this directory.
        for (var entry : processes.entrySet()) {
            if (directory.equals(entry.getKey())) {
                return;
            }
        }

        // start new services
        var newProcesses = startProcesses(directory);
        if (newProcesses != null && !newProcesses.isEmpty()) {
            var scanner = newProcesses.scanner;
            var indexer = newProcesses.indexer;

            processes.put(directory, newProcesses);
            if (scanner != null) {
                scanner.startNotify();
            }
            if (indexer != null) {
                indexer.startNotify();
            }
        }
    }

    @Override
    public synchronized void stop(@NotNull VirtualFile directory, boolean waitForTermination) {
        var entry = processes.remove(directory);
        if (entry != null) {
            stopLocked(entry, waitForTermination ? 1_000 : 0, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public synchronized void refreshForOpenProjects() {
        assert !ApplicationManager.getApplication().isDispatchThread();

        try {
            doRefreshForOpenProjectsLocked();
        } finally {
            if (!ApplicationManager.getApplication().isDisposed()) {
                var messageBus = ApplicationManager.getApplication().getMessageBus();
                messageBus.syncPublisher(AppLandCommandLineListener.TOPIC).afterRefreshForProjects();
            }
        }
    }

    @Override
    public void refreshForOpenProjectsInBackground() {
        ApplicationManager.getApplication().executeOnPooledThread(this::refreshForOpenProjects);
    }

    @Override
    public void restartProcessesInBackground() {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            stopAll(true);
            refreshForOpenProjects();
        });
    }

    @Override
    public @NotNull List<VirtualFile> getActiveRoots() {
        // We're not using "synchronized" because the ConcurrentHashMap does not require it,
        // and we must avoid possible deadlocks.
        // Refer to https://github.com/getappmap/appmap-intellij-plugin/issues/586.
        return List.copyOf(processes.keySet());
    }

    @Override
    public @Nullable VirtualFile getActiveRoot(@NotNull VirtualFile appMapFile) {
        return getActiveRoots().stream()
                .sorted(Comparator.<VirtualFile>comparingInt(root -> root.getPath().length()).reversed())
                .filter(root -> VfsUtilCore.isAncestor(root, appMapFile, false))
                .findFirst()
                .orElse(null);
    }

    @Override
    public void stopAll(boolean waitForTermination) {
        stopAll(waitForTermination ? 1_000 : 0, TimeUnit.MILLISECONDS);
    }

    @Override
    public void stopAll(int timeout, @NotNull TimeUnit timeUnit) {
        List<CliProcesses> activeProcesses;
        synchronized (this) {
            activeProcesses = List.copyOf(processes.values());
            processes.clear();
        }

        for (var processes : activeProcesses) {
            try {
                stopLocked(processes, timeout, timeUnit);
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
    public @Nullable GeneralCommandLine createAppMapJsonRpcCommand() {
        var cmd = createAppMapCommand("rpc", "--port", "0");
        if (cmd == null) {
            return null;
        }

        return applyServiceEnvironment(cmd);
    }

    // We're not synchronizing, because some IDE threads display or use the result of toString
    // and this must not create deadlocks.
    @Override
    public /*synchronized*/ String toString() {
        return "DefaultCommandLineService{" +
                "processes size=" + processes.size() + ", " +
                "processes=" + processes +
                '}';
    }

    /**
     * Stops processes, but does not modify {@link #processes}. It's the responsibility of the caller to update it.
     *
     * @param processes Processes to stop
     * @param timeout   Timeout to wait for process termination.
     * @param timeUnit  Unit of timeout.
     */
    private void stopLocked(@NotNull CliProcesses processes, int timeout, @NotNull TimeUnit timeUnit) {
        stopProcess(processes.indexer, timeout, timeUnit);
        stopProcess(processes.scanner, timeout, timeUnit);
    }

    @Override
    public void dispose() {
        var application = ApplicationManager.getApplication();
        if (application.isDisposed()) {
            // application shutdown in progress
            stopAll(false);
        } else if (!application.isDispatchThread()) {
            // dispose not called on EDT
            stopAll(false);
        } else {
            // Dispose called on EDT, e.g. when the AppMap plugin is unloaded.
            // We must not call stopAll on the EDT to avoid deadlocks, see #597.
            application.executeOnPooledThread(() -> stopAll(false));
        }
    }

    // extracted as method to allow overriding and testing it
    protected void requestVirtualFileRefresh(@NotNull Path path) {
        VfsUtil.markDirtyAndRefresh(true, false, false, path.toFile());
    }

    @TestOnly
    synchronized @Nullable CliProcesses getProcesses(@NotNull VirtualFile directory) {
        return processes.get(directory);
    }

    /**
     * Launch processes for the given directory.
     *
     * @param directory Directory
     * @return Reference to the launched processes if the operation was successful.
     */
    private @Nullable CliProcesses startProcesses(@NotNull VirtualFile directory) throws ExecutionException {
        var indexer = startIndexerProcess(directory);
        try {
            var scanner = startScannerProcesses(directory);
            return new CliProcesses(indexer, scanner);
        } catch (ExecutionException e) {
            LOG.debug("Error starting scanner process", e);
            if (indexer != null) {
                stopProcess(indexer, 0, TimeUnit.MILLISECONDS);
            }
            return null;
        }
    }

    /**
     * Starts a new process for an already managed directory.
     * This is used to restart a crashed process.
     *
     * @param type             Type of the process to restart
     * @param directory        Directory of the restarted process
     * @param nextRestartDelay The next restart delay to attach to the new process
     * @throws ExecutionException If the process could not be started
     */
    private synchronized void restartDirectoryProcess(@NotNull ProcessType type,
                                                      @NotNull VirtualFile directory,
                                                      long nextRestartDelay) throws ExecutionException {
        var directoryProcesses = processes.get(directory);
        if (directoryProcesses != null) {
            switch (type) {
                case Indexer:
                    assert directoryProcesses.indexer == null || directoryProcesses.indexer.isProcessTerminated();

                    var indexer = startIndexerProcess(directory);
                    directoryProcesses.indexer = indexer;
                    if (indexer != null) {
                        NEXT_RESTART_DELAY.set(indexer, nextRestartDelay);
                        indexer.startNotify();
                    }
                    break;
                case Scanner:
                    assert directoryProcesses.scanner == null || directoryProcesses.scanner.isProcessTerminated();

                    var scanner = startScannerProcesses(directory);
                    directoryProcesses.scanner = scanner;
                    if (scanner != null) {
                        NEXT_RESTART_DELAY.set(scanner, nextRestartDelay);
                        scanner.startNotify();
                    }
                    break;
            }
        }
    }

    /**
     * Launch a new indexer process for the given directory.
     *
     * @param directory Directory
     */
    private @Nullable KillableProcessHandler startIndexerProcess(@NotNull VirtualFile directory) throws ExecutionException {
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

        var workingDir = AppMapVfsUtils.asNativePath(directory);
        var watchedDir = findWatchedAppMapDirectory(workingDir);
        prepareWatchedDirectory(watchedDir);

        var process = startProcess(workingDir, indexerPath.toString(), "index",
                "--verbose",
                "--watch",
                "--appmap-dir", watchedDir.toString());
        process.addProcessListener(LoggingProcessAdapter.INSTANCE);
        process.addProcessListener(new RestartProcessListener(directory, process, ProcessType.Indexer, this), this);
        process.addProcessListener(new IndexEventsProcessListener(), this);
        return process;
    }

    /**
     * Launch a new scanner process for the given directory.
     *
     * @param directory Directory
     */
    private @Nullable KillableProcessHandler startScannerProcesses(@NotNull VirtualFile directory) throws ExecutionException {
        if (!isSupported()) {
            return null;
        }

        // don't launch for in-memory directories in unit test mode
        if (ApplicationManager.getApplication().isUnitTestMode() && directory.getFileSystem() instanceof TempFileSystem) {
            return null;
        }

        var scannerPath = AppLandDownloadService.getInstance().getDownloadFilePath(CliTool.Scanner);
        if (scannerPath == null || Files.notExists(scannerPath)) {
            return null;
        }

        var workingDir = AppMapVfsUtils.asNativePath(directory);
        var watchedDir = findWatchedAppMapDirectory(workingDir);
        prepareWatchedDirectory(watchedDir);

        var process = startProcess(workingDir, scannerPath.toString(), "scan", "--watch", "--appmap-dir", watchedDir.toString());
        process.addProcessListener(LoggingProcessAdapter.INSTANCE);
        process.addProcessListener(new RestartProcessListener(directory, process, ProcessType.Scanner, this), this);
        return process;
    }

    private void doRefreshForOpenProjectsLocked() {
        var enabledRoots = VfsUtilCore.createCompactVirtualFileSet();
        for (var project : ProjectManager.getInstance().getOpenProjects()) {
            var projectRoots = DumbService.getInstance(project).runReadActionInSmartMode(() -> {
                return AppMapFiles.findAppMapConfigFiles(project).stream()
                        .map(VirtualFile::getParent)
                        .filter(VirtualFile::isValid)
                        .collect(Collectors.toSet());
            });
            enabledRoots.addAll(projectRoots);
        }

        // Remove processes of roots, which no longer have a matching content root in a project or which don't match the
        // settings anymore.
        // We need to launch the scanner when "enableFindings" changes. We're iterating on a copy, because stop() is
        // called inside the loop and modifies "processes"
        for (var entry : List.copyOf(processes.entrySet())) {
            var activeRoot = entry.getKey();
            if (!enabledRoots.contains(activeRoot)) {
                try {
                    stop(activeRoot, false);
                } catch (Exception e) {
                    LOG.warn("Error stopping processes for root: " + activeRoot.getPath());
                }
            }
        }

        // launch missing cli processes
        for (var root : enabledRoots) {
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
        var appMapConfig = AppMapConfigFile.parseConfigFile(baseDirectory.resolve(AppMapFiles.APPMAP_YML));
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

    /**
     * Create AppMap directory if it does not exist yet.
     */
    private static void prepareWatchedDirectory(Path watchedDir) {
        if (Files.notExists(watchedDir)) {
            try {
                Files.createDirectories(watchedDir);
            } catch (Exception e) {
                LOG.debug("Failed to create AppMap directory: " + watchedDir, e);
            }
        }
    }

    private static boolean isSupported() {
        return SystemInfo.isMac && (CpuArch.isIntel64() || CpuArch.isArm64())
                || SystemInfo.isLinux && CpuArch.isIntel64()
                || SystemInfo.isWindows && CpuArch.isIntel64();
    }

    static @NotNull KillableProcessHandler startProcess(@NotNull Path workingDir,
                                                        @NotNull String... commandLine) throws ExecutionException {

        if (!Files.isDirectory(workingDir)) {
            throw new IllegalStateException("Directory does not exist: " + workingDir);
        }

        var command = new GeneralCommandLine(commandLine)
                .withWorkDirectory(workingDir.toString());

        return new KillableProcessHandler(applyServiceEnvironment(command)) {
            @Override
            protected BaseOutputReader.@NotNull Options readerOptions() {
                return BaseOutputReader.Options.BLOCKING;
            }
        };
    }

    private static void stopProcess(@Nullable KillableProcessHandler process, int timeout, @NotNull TimeUnit timeUnit) {
        if (process == null) {
            return;
        }

        NEXT_RESTART_DELAY.set(process, 0L);
        try {
            var shutdownRunnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        process.setShouldKillProcessSoftly(false);
                        process.destroyProcess();
                        process.waitFor(500);
                    } catch (WinpException e) {
                        // https://github.com/getappmap/appmap-intellij-plugin/issues/706
                        // On Windows 10 and later, ctrl+c is sent to the process as first attempt.
                        // If this attempt takes longer than 5s, then a WinpException is thrown.
                        LOG.warn("Failed to destroyProcess, falling back to forced process termination", e);
                    } finally {
                        if (!process.isProcessTerminated()) {
                            process.killProcess();
                        }

                        if (timeout > 0) {
                            process.waitFor(timeUnit.toMillis(timeout));
                        }
                    }
                }
            };

            if (timeout > 0) {
                ApplicationManager.getApplication().executeOnPooledThread(shutdownRunnable).get();
            } else {
                shutdownRunnable.run();
            }
        } catch (Exception e) {
            LOG.warn("Error shutting down process: " + process, e);
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

    /**
     * Configure the environment settings of the command line with the user-defined settings.
     */
    private static GeneralCommandLine applyServiceEnvironment(@NotNull GeneralCommandLine commandLine) {
        var settings = AppMapApplicationSettingsService.getInstance();
        var appMapKey = settings.getApiKey();
        var openAIKey = AppMapSecureApplicationSettingsService.getInstance().getOpenAIKey();

        var environmentType = settings.isCliPassParentEnv()
                ? GeneralCommandLine.ParentEnvironmentType.CONSOLE
                : GeneralCommandLine.ParentEnvironmentType.NONE;

        return commandLine.withParentEnvironmentType(environmentType)
                .withEnvironment(settings.getCliEnvironment())
                .withEnvironment(isNotEmpty(appMapKey) ? Map.of("APPMAP_API_KEY", appMapKey) : Map.of())
                .withEnvironment(isNotEmpty(openAIKey) ? Map.of("OPENAI_API_KEY", openAIKey) : Map.of())
                .withEnvironment(createProxyEnvironment());
    }

    /**
     * Creates a map of proxy environment variable settings.
     * The following variables are supported:
     * - http_proxy
     * - https_proxy
     * - no_proxy
     * <p>
     * SOCKS proxy settings of the IDE are not supported. In this case, an empty map is returned.
     * <p>
     * Autoconfigured PAC proxy settings are not supported. In this case, an empty map is returned.
     * <p>
     * If the settings define proxy authentication settings, then username and password are part of the proxy URLs.
     *
     * @return Map of UNIX-style proxy environment variables, based on the IDE's settings.
     * If the IDE does not define proxy settings, then an empty map is returned.
     */
    static @NotNull Map<String, String> createProxyEnvironment() {
        var settings = HttpConfigurable.getInstance();
        if (!settings.USE_HTTP_PROXY || settings.PROXY_TYPE_IS_SOCKS) {
            return Map.of();
        }

        // we're following the SDK's com.intellij.util.net.HttpConfigurable.getJvmProperties
        var proxyLogin = settings.getProxyLogin();
        var userAndPassword = settings.KEEP_PROXY_PASSWORD && StringUtil.isNotEmpty(proxyLogin)
                ? proxyLogin + ":" + StringUtil.defaultIfEmpty(settings.getPlainProxyPassword(), "")
                : null;

        try {
            var proxyURL = new URI("http", userAndPassword, settings.PROXY_HOST, settings.PROXY_PORT, null, null, null)
                    .toURL()
                    .toExternalForm();
            return Map.of(
                    "http_proxy", proxyURL,
                    "https_proxy", proxyURL,
                    "no_proxy", StringUtil.defaultIfEmpty(settings.PROXY_EXCEPTIONS, ""));
        } catch (MalformedURLException | URISyntaxException e) {
            LOG.debug("Unable to create proxy settings for AppMap command line", e);
            return Map.of();
        }
    }

    /**
     * Indexer and scanner processes which are active for a single base directory.
     * It's possible that a reference is {@code null}, e.g. due to a crash and too many restart attempts.
     */
    @Data
    protected static final class CliProcesses {
        private volatile @Nullable KillableProcessHandler indexer;
        private volatile @Nullable KillableProcessHandler scanner;

        private CliProcesses(@Nullable KillableProcessHandler indexer, @Nullable KillableProcessHandler scanner) {
            this.indexer = indexer;
            this.scanner = scanner;
        }

        boolean isEmpty() {
            return indexer == null && scanner == null;
        }
    }

    /**
     * Listen to index events of the indexer.
     */
    private class IndexEventsProcessListener extends ProcessAdapter {
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
    }

    /**
     * Process listener to handle restarts when the given process terminates unexpectedly.
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    private class RestartProcessListener extends ProcessAdapter {
        private final @NotNull VirtualFile directory;
        private final @NotNull KillableProcessHandler process;
        private final @NotNull ProcessType type;
        private final @NotNull Disposable parentDisposable;

        public RestartProcessListener(@NotNull VirtualFile directory,
                                      @NotNull KillableProcessHandler process,
                                      @NotNull ProcessType type,
                                      @NotNull Disposable parentDisposable) {
            this.directory = directory;
            this.process = process;
            this.type = type;
            this.parentDisposable = parentDisposable;
        }

        @Override
        public void processTerminated(@NotNull ProcessEvent event) {
            // NEXT_RESTART_DELAY is stored in the process's user data.
            // It's set to 0 when the process is stopped by this service.
            long currentRestartDelay = NEXT_RESTART_DELAY.get(process, INITIAL_RESTART_DELAY_MILLIS);

            // don't restart if max attempts were reached or if restarts were disabled
            var noRestartNeeded = currentRestartDelay <= 0 || currentRestartDelay > MAX_RESTART_DELAY_MILLIS;
            if (noRestartNeeded) {
                synchronized (DefaultCommandLineService.this) {
                    var entry = processes.get(directory);
                    if (entry != null) {
                        // We're only resetting the process entry if it was referencing the terminated process
                        // For example, if process shutdown takes a long time, then closing and reopening a project
                        // must not reset the processes incorrectly.
                        switch (type) {
                            case Indexer:
                                if (process.equals(entry.indexer)) {
                                    entry.indexer = null;
                                }
                                break;
                            case Scanner:
                                if (process.equals(entry.scanner)) {
                                    entry.scanner = null;
                                }
                                break;
                        }

                        if (entry.isEmpty()) {
                            processes.remove(directory);
                        }
                    }
                }
                return;
            }

            // schedule next restart
            LOG.debug("Scheduling AppMap process restart. Type: " + type + ", command line: " + process.getCommandLine());
            AppExecutorUtil.getAppScheduledExecutorService().schedule(() -> {
                try {
                    var nextRestartDelay = (long) ((double) currentRestartDelay * NEXT_RESTART_FACTOR);
                    DefaultCommandLineService.this.restartDirectoryProcess(type, directory, nextRestartDelay);
                } catch (Exception e) {
                    LOG.debug("Error restarting process. Type: " + type + ", command line: " + process.getCommandLine(), e);
                }
            }, currentRestartDelay, TimeUnit.MILLISECONDS);
        }
    }

    private enum ProcessType {
        Indexer, Scanner
    }
}
