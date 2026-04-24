package appland.rpcService;

import appland.cli.*;
import appland.config.AppMapConfigFileListener;
import appland.files.AppMapFiles;
import appland.settings.AppMapApplicationSettingsService;
import appland.settings.AppMapSettingsListener;
import appland.utils.GsonUtils;
import appland.utils.UserLog;
import com.google.gson.JsonObject;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.process.*;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootEvent;
import com.intellij.openapi.roots.ModuleRootListener;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Alarm;
import com.intellij.util.SingleAlarm;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread;
import com.intellij.util.concurrency.annotations.RequiresReadLock;
import com.intellij.util.io.BaseOutputReader;
import com.intellij.util.io.HttpRequests;
import net.jcip.annotations.GuardedBy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jvnet.winp.WinpException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * This implementation restarts the JSON-RPC server up to {@link #MAX_CRASH_RESTARTS} times if it
 * terminates unexpectedly. The following configuration changes trigger an intentional restart:
 * - Change of the AppMap API key
 * - Download or update of an AppMap CLI binary
 * - Add/remove/update of an appmap.yml file
 */
@SuppressWarnings("UnstableApiUsage")
public class DefaultAppLandJsonRpcService implements AppLandJsonRpcService, AppLandDownloadListener {
    private static final Logger LOG = Logger.getInstance(DefaultAppLandJsonRpcService.class);
    private static final Pattern PORT_PATTERN = Pattern.compile("^Running JSON-RPC server on port: (\\d+)$");
    private static final long INITIAL_CRASH_RESTART_DELAY_MILLIS = 1_000;
    private static final double CRASH_RESTART_DELAY_FACTOR = 1.5;
    private static final int MAX_CRASH_RESTARTS = 3;

    private enum ServerState {
        /**
         * No process running; can be started.
         */
        STOPPED,
        /**
         * Process launched; waiting for port announcement on stdout.
         */
        STARTING,
        /**
         * Port announced; server is ready to accept RPC calls.
         */
        RUNNING,
        /**
         * Process is being killed by an explicit stop/restart call.
         */
        STOPPING,
        /**
         * Process crashed; a scheduled task will attempt a restart.
         */
        CRASH_RESTARTING,
        /**
         * Service disposed; no further starts allowed.
         */
        DISPOSED
    }

    private final @NotNull Project project;
    private final @Nullable ScheduledFuture<?> pollingFuture;
    private final SingleAlarm updateServerSettingsAlarm = new SingleAlarm(this::updateServerSettings,
            1_000, this, Alarm.ThreadToUse.POOLED_THREAD);

    // Fast non-locking guard used in hot paths where acquiring "this" would be heavy.
    private volatile boolean isDisposed;

    @GuardedBy("this")
    private ServerState state = ServerState.STOPPED;
    @GuardedBy("this")
    protected @Nullable KillableProcessHandler currentProcess = null;
    // Preserved across restarts so the server binds to the same port every time.
    @GuardedBy("this")
    private @Nullable Integer lastKnownPort = null;
    // Reset to 0 whenever the server reaches RUNNING; incremented on each crash.
    @GuardedBy("this")
    private int crashRestartCount = 0;

    private static final @NotNull AtomicInteger jsonRpcRequestCounter = new AtomicInteger(0);
    private static final @NotNull UserLog OUTPUT_LOG_STREAM = new UserLog("appmap-json-rpc.log");

    public DefaultAppLandJsonRpcService(@NotNull Project project) {
        this.project = project;

        var application = ApplicationManager.getApplication();

        var applicationBus = application.getMessageBus().connect(this);
        applicationBus.subscribe(AppLandDownloadListener.TOPIC, this);
        applicationBus.subscribe(AppMapConfigFileListener.TOPIC, (AppMapConfigFileListener) this::triggerSendConfigurationSet);
        applicationBus.subscribe(AppMapSettingsListener.TOPIC, new AppMapSettingsListener() {
            @Override
            public void modelConfigChange() {
                restartServerAsync();
            }

            @Override
            public void secureModelConfigChange(@NotNull String key) {
                restartServerAsync();
            }
        });

        var projectBus = project.getMessageBus().connect(this);
        projectBus.subscribe(ModuleRootListener.TOPIC, new ModuleRootListener() {
            @Override
            public void rootsChanged(@NotNull ModuleRootEvent event) {
                triggerSendConfigurationSet();
            }
        });

        if (!application.isUnitTestMode()) {
            this.pollingFuture = AppExecutorUtil.getAppScheduledExecutorService().scheduleWithFixedDelay(
                    this::triggerSendConfigurationSet, 30_000, 30_000, TimeUnit.MILLISECONDS);
        } else {
            this.pollingFuture = null;
        }
    }

    @Override
    public void dispose() {
        isDisposed = true;

        try {
            var future = pollingFuture;
            if (future != null) {
                future.cancel(true);
            }
        } finally {
            KillableProcessHandler process;
            synchronized (this) {
                state = ServerState.DISPOSED;
                process = currentProcess;
                currentProcess = null;
            }

            var application = ApplicationManager.getApplication();
            if (application.isDisposed() || !application.isDispatchThread()) {
                killProcessAndWait(process);
            } else {
                application.executeOnPooledThread(() -> killProcessAndWait(process));
            }
        }
    }

    @Override
    public synchronized boolean isServerRunning() {
        return state == ServerState.RUNNING;
    }

    @Override
    public void startServer() {
        startServerInternal();
    }

    @Override
    @RequiresBackgroundThread
    public void stopServer() {
        stopCurrentProcessSync();
    }

    @Override
    public void restartServer() {
        restartServerSync();
    }

    @Override
    public synchronized @Nullable Integer getServerPort() {
        return state == ServerState.RUNNING ? lastKnownPort : null;
    }

    @Override
    @NotNull
    public List<NavieThreadQueryV1Response.NavieThread> queryNavieThreads(@NotNull NavieThreadQueryV1Params params) throws IOException {
        var serverUrl = getServerUrl();
        if (serverUrl == null) {
            throw new IOException("Navie JSON-RPC server is not running");
        }

        var response = sendJsonRpcMessage(serverUrl, "v1.navie.thread.query", params);
        if (response == null) {
            throw new IOException("Received empty response from JSON-RPC server");
        }
        return GsonUtils.GSON.fromJson(response, NavieThreadQueryV1Response.class).result();
    }

    @Override
    public void downloadFinished(@NotNull CliTool type, @NotNull AppMapDownloadStatus status) {
        if (!status.isSuccessful() || !CliTool.AppMap.equals(type)) {
            return;
        }
        synchronized (this) {
            // A running server is fine with the old binary; it will pick up the update on the next session.
            if (state == ServerState.RUNNING) {
                return;
            }
        }
        restartServerAsync();
    }

    private void startServerInternal() {
        Integer portToUse;
        synchronized (this) {
            if (state != ServerState.STOPPED && state != ServerState.CRASH_RESTARTING) {
                LOG.debug("Not starting JSON-RPC server, state is " + state);
                return;
            }
            if (isDisposed || project.isDisposed()) {
                LOG.debug("Not starting JSON-RPC server, service is disposed");
                return;
            }
            portToUse = lastKnownPort;
        }

        var commandLine = AppLandCommandLineService.getInstance().createAppMapJsonRpcCommand(portToUse);
        if (commandLine == null) {
            LOG.debug("Unable to launch JSON-RPC server, because CLI command is unavailable.");
            return;
        }

        commandLine = commandLine.withEnvironment("APPMAP_CODE_EDITOR", createCodeEditorInfo());

        KillableProcessHandler process;
        try {
            synchronized (this) {
                // Re-check under lock after the (potentially slow) command-line creation.
                if (state != ServerState.STOPPED && state != ServerState.CRASH_RESTARTING) {
                    return;
                }
                if (isDisposed || project.isDisposed()) {
                    return;
                }

                process = new KillableProcessHandler(commandLine) {
                    @NotNull
                    @Override
                    protected BaseOutputReader.Options readerOptions() {
                        return BaseOutputReader.Options.forMostlySilentProcess();
                    }
                };
                process.addProcessListener(processListener, this);
                currentProcess = process;
                state = ServerState.STARTING;
            }
            process.startNotify();
        } catch (ExecutionException e) {
            LOG.debug("Failed to launch AppMap JSON-RPC server: " + commandLine.getCommandLineString(), e);
        }
    }

    @RequiresBackgroundThread
    private void stopCurrentProcessSync() {
        ApplicationManager.getApplication().assertIsNonDispatchThread();

        KillableProcessHandler process;
        synchronized (this) {
            switch (state) {
                case STOPPED:
                case STOPPING:
                case DISPOSED:
                    return;
                case CRASH_RESTARTING:
                    // Cancel the pending restart; the scheduled task checks state and will no-op.
                    state = ServerState.STOPPED;
                    return;
                default:
                    // STARTING or RUNNING: take ownership of the process and begin stopping.
                    process = currentProcess;
                    currentProcess = null;
                    state = ServerState.STOPPING;
            }
        }

        killProcessAndWait(process);

        synchronized (this) {
            if (state == ServerState.STOPPING) {
                state = ServerState.STOPPED;
            }
        }

        if (!isDisposed && !project.isDisposed()) {
            project.getMessageBus().syncPublisher(AppLandJsonRpcListener.TOPIC).serverStopped();
        }
    }

    @RequiresBackgroundThread
    private void restartServerSync() {
        ApplicationManager.getApplication().assertIsNonDispatchThread();

        // No try/finally needed: stopCurrentProcessSync swallows all exceptions internally,
        // and serverStarted fires automatically from onPortAnnounced when the new process announces its port.
        project.getMessageBus().syncPublisher(AppLandJsonRpcListener.TOPIC).beforeServerRestart();
        stopCurrentProcessSync();
        if (!project.isDisposed()) {
            synchronized (this) {
                // Intentional restart: don't count against the crash budget.
                crashRestartCount = 0;
            }
            startServerInternal();
        }
    }

    private void restartServerAsync() {
        if (isDisposed || project.isDisposed()) {
            return;
        }

        var application = ApplicationManager.getApplication();
        if (application.isDispatchThread()) {
            application.executeOnPooledThread(this::restartServerSync);
        } else {
            restartServerSync();
        }
    }

    private void onPortAnnounced(@NotNull ProcessHandler process, int port) {
        synchronized (this) {
            if (process != currentProcess || state != ServerState.STARTING) {
                return;
            }
            lastKnownPort = port;
            state = ServerState.RUNNING;
            crashRestartCount = 0;
        }
        // Push config before notifying listeners so the initial state is already set when they react.
        updateServerSettings();
        updateModelConfig();
        if (!isDisposed && !project.isDisposed()) {
            project.getMessageBus().syncPublisher(AppLandJsonRpcListener.TOPIC).serverStarted();
        }
    }

    private void onProcessTerminated(@NotNull ProcessHandler process) {
        OUTPUT_LOG_STREAM.log("AppMap JSON-RPC server terminated\n\n");
        if (LOG.isDebugEnabled()) {
            LOG.debug("AppMap JSON-RPC server terminated");
        }

        boolean scheduleCrashRestart;
        long restartDelay;
        synchronized (this) {
            if (process != currentProcess) {
                // Stale event from a process we already replaced or explicitly stopped.
                return;
            }
            if (state == ServerState.STOPPING || state == ServerState.DISPOSED) {
                // Intentional stop; stopCurrentProcessSync/dispose owns the transition.
                return;
            }

            // Unexpected termination (crash) while STARTING or RUNNING.
            currentProcess = null;

            boolean canRestart = !isDisposed && !project.isDisposed() && crashRestartCount < MAX_CRASH_RESTARTS;
            if (canRestart) {
                crashRestartCount++;
                restartDelay = (long) (INITIAL_CRASH_RESTART_DELAY_MILLIS
                        * Math.pow(CRASH_RESTART_DELAY_FACTOR, crashRestartCount - 1));
                state = ServerState.CRASH_RESTARTING;
                scheduleCrashRestart = true;
            } else {
                state = ServerState.STOPPED;
                scheduleCrashRestart = false;
                restartDelay = 0;
            }
        }

        if (scheduleCrashRestart) {
            AppExecutorUtil.getAppScheduledExecutorService().schedule(() -> {
                if (isDisposed || project.isDisposed()) {
                    return;
                }
                synchronized (DefaultAppLandJsonRpcService.this) {
                    if (state != ServerState.CRASH_RESTARTING) {
                        return;
                    }
                    state = ServerState.STOPPED;
                }
                startServerInternal();
            }, restartDelay, TimeUnit.MILLISECONDS);
        }
    }

    private static void killProcessAndWait(@Nullable KillableProcessHandler process) {
        if (process == null) {
            return;
        }
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
        }
    }

    private @Nullable String getServerUrl() {
        var port = getServerPort();
        return port != null ? "http://127.0.0.1:" + port : null;
    }

    private void triggerSendConfigurationSet() {
        updateServerSettingsAlarm.cancelAndRequest();
    }

    private void updateServerSettings() {
        ApplicationManager.getApplication().assertIsNonDispatchThread();
        var serverUrl = getServerUrl();
        if (serverUrl == null) {
            LOG.debug("Unable to send \"v2.configuration.set\" because JSON-RPC service is unavailable.");
            return;
        }
        sendConfigurationSetMessage(serverUrl);
    }

    private void updateModelConfig() {
        ApplicationManager.getApplication().assertIsNonDispatchThread();
        var serverUrl = getServerUrl();
        if (serverUrl == null) {
            LOG.debug("Unable to send model config because JSON-RPC service is unavailable.");
            return;
        }
        sendNavieModelsAddMessage(serverUrl);
        sendNavieModelsSelectMessage(serverUrl);
    }

    private void sendNavieModelsAddMessage(@NotNull String serverUrl) {
        List<AppLandModelInfoProvider.ModelInfo> allModels = null;

        for (var provider : AppLandModelInfoProvider.EP_NAME.getExtensionList()) {
            try {
                var models = provider.getModelInfo();
                if (models != null) {
                    if (allModels == null) {
                        allModels = new ArrayList<>(models);
                    } else {
                        allModels.addAll(models);
                    }
                }
            } catch (IOException e) {
                LOG.debug("Failed to get models from provider: " + provider.getClass().getName(), e);
            }
        }

        if (allModels != null) {
            try {
                sendJsonRpcMessage(serverUrl, "v1.navie.models.add", allModels);
            } catch (IOException e) {
                LOG.debug("Failed to send \"v1.navie.models.add\" message", e);
            }
        }
    }

    private void sendNavieModelsSelectMessage(@NotNull String serverUrl) {
        var selectedModel = AppMapApplicationSettingsService.getInstance().getSelectedAppMapModel();
        if (StringUtil.isNotEmpty(selectedModel)) {
            try {
                sendJsonRpcMessage(serverUrl, "v1.navie.models.select", new NavieModelsSelectV1Params(selectedModel));
            } catch (IOException e) {
                LOG.debug("Failed to send \"v1.navie.models.select\" message", e);
            }
        }
    }

    private record ProjectConfig(Collection<VirtualFile> contentRoots, Collection<VirtualFile> configFiles) {
    }

    private void sendConfigurationSetMessage(@NotNull String serverUrl) {
        DumbService.getInstance(project).waitForSmartMode();
        var config = ReadAction.compute(() -> new ProjectConfig(
                List.of(AppMapFiles.findTopLevelContentRoots(project)),
                AppMapFiles.findAppMapConfigFiles(project)));

        var contentRoots = config.contentRoots();
        var jsonConfigFiles = config.configFiles();

        try {
            sendJsonRpcMessage(serverUrl,
                    "v2.configuration.set",
                    new SetConfigurationV2Params(mapToLocalPaths(contentRoots), mapToLocalPaths(jsonConfigFiles)));
        } catch (IOException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Error sending request v2.configuration.set", e);
            }
            // Fallback to v1 API.
            try {
                sendJsonRpcMessage(serverUrl,
                        "v1.configuration.set",
                        new SetConfigurationV1Params(mapToLocalPaths(jsonConfigFiles)));
            } catch (IOException ex) {
                LOG.warn("Error sending fallback request v1.configuration.set", ex);
            }
        } finally {
            if (!isDisposed && !project.isDisposed()) {
                project.getMessageBus()
                        .syncPublisher(AppLandJsonRpcListener.TOPIC)
                        .serverConfigurationUpdated(contentRoots, jsonConfigFiles);
            }
        }
    }

    /**
     * @throws IOException If a response other than 2xx was returned or if the JSON-RPC response contained an error code.
     */
    private static @Nullable JsonObject sendJsonRpcMessage(@NotNull String serverUrl,
                                                           @NotNull String method,
                                                           @NotNull Object params) throws IOException {
        var payload = new JsonObject();
        payload.addProperty("jsonrpc", "2.0");
        payload.addProperty("method", method);
        payload.addProperty("id", jsonRpcRequestCounter.incrementAndGet());
        payload.add("params", GsonUtils.GSON.toJsonTree(params));

        var json = GsonUtils.GSON.toJson(payload);
        if (LOG.isTraceEnabled()) {
            LOG.trace("Sending JSON-RPC message: " + json);
        }

        var responseData = HttpRequests
                .post(serverUrl, "application/json")
                .connect(request -> {
                    request.write(json);
                    return request.readString();
                });

        if (!responseData.isEmpty()) {
            var response = GsonUtils.GSON.fromJson(responseData, JsonObject.class);
            if (response != null && response.has("error")) {
                var error = response.get("error");
                if (error.isJsonObject() && ((JsonObject) error).has("code")) {
                    throw new IOException("Error response sending JSON-RPC request: " + error);
                }
            }
            return response;
        }

        return null;
    }

    @RequiresReadLock
    private static @NotNull Collection<@NotNull String> mapToLocalPaths(@NotNull Collection<VirtualFile> files) {
        return files.stream()
                .map(file -> file.getFileSystem().getNioPath(file))
                .filter(Objects::nonNull)
                .map(path -> path.normalize().toString())
                .collect(Collectors.toList());
    }

    protected static @NotNull String createCodeEditorInfo() {
        var info = ApplicationInfo.getInstance();
        return info.getFullApplicationName() + " by " + info.getCompanyName();
    }

    private final ProcessAdapter processListener = new ProcessAdapter() {
        @Override
        public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
            OUTPUT_LOG_STREAM.log(event.getText());
            if (LOG.isDebugEnabled() && outputType != ProcessOutputTypes.SYSTEM) {
                LOG.debug("JSON-RPC: " + event.getText().trim());
            }

            var match = PORT_PATTERN.matcher(event.getText().trim());
            if (match.matches()) {
                var port = StringUtil.parseInt(match.group(1), -1);
                if (port > 0) {
                    onPortAnnounced(event.getProcessHandler(), port);
                }
            }
        }

        @Override
        public void processTerminated(@NotNull ProcessEvent event) {
            onProcessTerminated(event.getProcessHandler());
        }
    };
}
