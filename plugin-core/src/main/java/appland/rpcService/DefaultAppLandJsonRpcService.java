package appland.rpcService;

import appland.cli.AppLandCommandLineService;
import appland.cli.AppLandDownloadListener;
import appland.cli.AppLandModelInfoProvider;
import appland.cli.CliTool;
import appland.config.AppMapConfigFileListener;
import appland.files.AppMapFiles;
import appland.settings.AppMapApplicationSettingsService;
import appland.settings.AppMapSettingsListener;
import appland.utils.GsonUtils;
import appland.utils.UserLog;
import com.google.gson.JsonObject;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.process.KillableProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootEvent;
import com.intellij.openapi.roots.ModuleRootListener;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Alarm;
import com.intellij.util.SingleAlarm;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread;
import com.intellij.util.concurrency.annotations.RequiresReadLock;
import com.intellij.util.io.BaseOutputReader;
import com.intellij.util.io.HttpRequests;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import appland.rpcService.NavieThreadQueryV1Params;
import appland.rpcService.NavieThreadQueryV1Response;
import lombok.Data;
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
 * This implementation restarts the JSON-RPC server up to three times if it terminates unexpectedly.
 * The following configuration changes trigger a restart:
 * - Change of the AppMap API key
 * - Download or update of an AppMap CLI binary
 * - Add/remove/update of an appmap.yml file
 */
@SuppressWarnings("UnstableApiUsage")
public class DefaultAppLandJsonRpcService implements AppLandJsonRpcService, AppLandDownloadListener {
    private static final Logger LOG = Logger.getInstance(DefaultAppLandJsonRpcService.class);
    // pattern to capture the code in STDOUT of the server process
    private static final Pattern PORT_PATTERN = Pattern.compile("^Running JSON-RPC server on port: (\\d+)$");
    // initial delay for the first restart attempt
    private static final long INITIAL_RESTART_DELAY_MILLIS = 5_000;
    // factor to calculate the next restart delay based on the previous delay
    private static final double NEXT_RESTART_FACTOR = 1.5;
    // three restarts at most (5_000 * 1.5^3 > 15_000)
    private static final long MAX_RESTART_DELAY_MILLIS = INITIAL_RESTART_DELAY_MILLIS * 3;

    private final @NotNull Project project;
    // future to control the lifetime of the appmap.yml polling
    private final @Nullable ScheduledFuture<?> pollingFuture;
    // debounce requests to send the configuration message to the server by 1s
    private final SingleAlarm updateServerSettingsAlarm = new SingleAlarm(this::updateServerSettings,
            1_000,
            this,
            Alarm.ThreadToUse.POOLED_THREAD);
    // debounce requests to send model configuration update messages by 1s
    private final SingleAlarm updateModelConfigAlarm = new SingleAlarm(this::updateModelConfig,
            1_000,
            this,
            Alarm.ThreadToUse.POOLED_THREAD);

    // flag to help avoid starting the server if we're already disposed
    private volatile boolean isDisposed;
    // a value of "0" indicates that process restart is disabled
    private volatile long nextRestartDelayMillis = INITIAL_RESTART_DELAY_MILLIS;
    @GuardedBy("this")
    protected volatile @Nullable JsonRpcServer jsonRpcServer = null;

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
            public void secureModelConfigChange() {
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
            // poll AppMap configuration files every 30s and send "v1.configuration.set",
            // but we don't want to update the model configuration.
            this.pollingFuture = AppExecutorUtil.getAppScheduledExecutorService().scheduleWithFixedDelay(this::triggerSendConfigurationSet,
                    30_000,
                    30_000,
                    TimeUnit.MILLISECONDS);
        } else {
            this.pollingFuture = null;
        }
    }

    @Override
    public void dispose() {
        this.isDisposed = true;

        // disable restarts
        this.nextRestartDelayMillis = 0L;

        try {
            var future = pollingFuture;
            if (future != null) {
                future.cancel(true);
            }
        } finally {
            stopServerAsync();
        }
    }

    @Override
    public synchronized boolean isServerRunning() {
        return jsonRpcServer != null;
    }

    @Override
    public void startServer() {
        startServer(null);
    }

    private void startServer(@Nullable Integer port) {
        if (isServerRunning()) {
            LOG.debug("AppMap JSON-RPC server is already running.");
            return;
        }

        if (isDisposed || project.isDisposed()) {
            LOG.debug("Unable to start JSON-RPC server, because the service is already disposed.");
            return;
        }

        var commandLine = AppLandCommandLineService.getInstance().createAppMapJsonRpcCommand(port);
        if (commandLine == null) {
            LOG.debug("Unable to launch JSON-RPC server, because CLI command is unavailable.");
            return;
        }

        commandLine = commandLine.withEnvironment("APPMAP_CODE_EDITOR", createCodeEditorInfo());

        synchronized (this) {
            if (isServerRunning()) {
                return;
            }

            try {
                var process = new KillableProcessHandler(commandLine) {
                    @NotNull
                    @Override
                    protected BaseOutputReader.Options readerOptions() {
                        return BaseOutputReader.Options.forMostlySilentProcess();
                    }
                };

                var jsonRpcServer = new JsonRpcServer(process);
                process.addProcessListener(new JsonRpcProcessListener(jsonRpcServer), this);

                this.jsonRpcServer = jsonRpcServer;

                // launch process and our process listener
                process.startNotify();
            } catch (ExecutionException e) {
                LOG.debug("Failed to launch AppMap JSON-RPC server, command: " + commandLine.getCommandLineString(), e);
            } finally {
                if (!isDisposed) {
                    project.getMessageBus().syncPublisher(AppLandJsonRpcListener.TOPIC).serverStarted();
                }
            }
        }
    }

    @Override
    public synchronized void stopServerAsync() {
        var server = jsonRpcServer;
        if (server != null) {
            this.jsonRpcServer = null;

            ApplicationManager.getApplication().executeOnPooledThread(() -> stopServerSync(server));
        }
    }

    @Override
    public void restartServer() {
        restartServerSync();
    }

    @Override
    public synchronized @Nullable Integer getServerPort() {
        var server = jsonRpcServer;
        return server != null ? server.jsonRpcPort : null;
    }

    private @Nullable String getServerUrl() {
        var port = getServerPort();
        return port != null ? "http://127.0.0.1:" + port : null;
    }
    /**
     * Queries existing Navie threads via JSON-RPC.
     * @param params RPC parameters
     * @return list of threads
     * @throws IOException on RPC error
     */
    @Override
    @NotNull
    public List<NavieThreadQueryV1Response.NavieThread> queryNavieThreads(@NotNull NavieThreadQueryV1Params params) throws IOException {
        String serverUrl = getServerUrl();
        if (serverUrl == null) {
            throw new IOException("Navie JSON-RPC server is not running");
        }

        try {
            var response = sendJsonRpcMessage(serverUrl, "v1.navie.thread.query", params);
            if (response == null) {
                throw new IOException("Received empty response from JSON-RPC server");
            }

            var result = GsonUtils.GSON.fromJson(response, NavieThreadQueryV1Response.class);
            return result.result();
        } catch (IOException e) {
            LOG.debug("Failed to send \"v1.navie.thread.query\" message", e);
        }

        return List.of();
    }

    @RequiresBackgroundThread
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

    @RequiresBackgroundThread
    private void restartServerSync() {
        ApplicationManager.getApplication().assertIsNonDispatchThread();

        var publisher = project.getMessageBus().syncPublisher(AppLandJsonRpcListener.TOPIC);

        Integer previousPort = null;
        try {
            publisher.beforeServerRestart();

            previousPort = stopServerSync(this.jsonRpcServer);
        } finally {
            if (!project.isDisposed()) {
                try {
                    startServer(previousPort);
                } finally {
                    publisher.serverRestarted();
                }
            }
        }
    }

    /**
     * Stops the server and waits for a short time before it's forcibly killed.
     *
     * @param server The server to stop, a {@code null} value is ignored.
     * @return Port used by the server, if a server was running.
     */
    @RequiresBackgroundThread
    private Integer stopServerSync(@Nullable JsonRpcServer server) {
        ApplicationManager.getApplication().assertIsNonDispatchThread();

        if (server == null) {
            return null;
        }

        var port = server.getJsonRpcPort();
        try {
            var process = server.processHandler;
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
        } finally {
            if (!isDisposed) {
                project.getMessageBus().syncPublisher(AppLandJsonRpcListener.TOPIC).serverStopped();
            }
        }
        return port;
    }

    @Override
    public void downloadFinished(@NotNull CliTool type, boolean success) {
        if (success && CliTool.AppMap.equals(type) && !isServerRunning()) {
            restartServerAsync();
        }
    }

    private void triggerSendConfigurationSet() {
        updateServerSettingsAlarm.cancelAndRequest();
    }

    private void triggerModelConfigUpdate() {
        updateModelConfigAlarm.cancelAndRequest();
    }

    /**
     * Updates the JSON-RPC server with the current settings by sending JSON-RPC messages.
     */
    private void updateServerSettings() {
        ApplicationManager.getApplication().assertIsNonDispatchThread();

        var serverUrl = getServerUrl();
        if (serverUrl == null) {
            LOG.debug("Unable to send \"v1.configuration.set\" because JSON-RPC service is unavailable.");
            return;
        }

        sendConfigurationSetMessage(serverUrl);
    }

    /**
     * Updates the JSON-RPC server with the current settings by sending JSON-RPC messages.
     */
    private void updateModelConfig() {
        ApplicationManager.getApplication().assertIsNonDispatchThread();

        var serverUrl = getServerUrl();
        if (serverUrl == null) {
            LOG.debug("Unable to send \"v1.configuration.set\" because JSON-RPC service is unavailable.");
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

    /**
     * Sends message "v1.configuration.set" to the currently running JSON-RPC server.
     */
    private void sendConfigurationSetMessage(String serverUrl) {
        var contentRootsWithConfigFiles = DumbService.getInstance(project).runReadActionInSmartMode(() -> {
            var contentRoots = List.of(AppMapFiles.findTopLevelContentRoots(project));
            var configFiles = AppMapFiles.findAppMapConfigFiles(project);
            return new Pair<Collection<VirtualFile>, Collection<VirtualFile>>(contentRoots, configFiles);
        });

        var contentRoots = contentRootsWithConfigFiles.first;
        var jsonConfigFiles = contentRootsWithConfigFiles.second;

        try {
            sendJsonRpcMessage(serverUrl,
                    "v2.configuration.set",
                    new SetConfigurationV2Params(mapToLocalPaths(contentRoots), mapToLocalPaths(jsonConfigFiles)));
        } catch (IOException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Error sending request v2.configuration.set", e);
            }

            // fallback to v1 API
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

    @Data
    protected static final class JsonRpcServer {
        @NotNull KillableProcessHandler processHandler;
        @Nullable Integer jsonRpcPort = null;
    }

    private class JsonRpcProcessListener extends ProcessAdapter {
        private final JsonRpcServer jsonRpcServer;

        private JsonRpcProcessListener(JsonRpcServer jsonRpcServer) {
            this.jsonRpcServer = jsonRpcServer;
        }

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
                    jsonRpcServer.jsonRpcPort = port;
                    triggerSendConfigurationSet();
                    triggerModelConfigUpdate();
                }
            }
        }

        @Override
        public void processTerminated(@NotNull ProcessEvent event) {
            OUTPUT_LOG_STREAM.log("AppMap JSON-RPC server terminated: " + event + "\n\n");
            if (LOG.isDebugEnabled()) {
                LOG.debug("AppMap JSON-RPC server terminated: " + event);
            }

            synchronized (DefaultAppLandJsonRpcService.this) {
                DefaultAppLandJsonRpcService.this.jsonRpcServer = null;
                jsonRpcServer.jsonRpcPort = null;
            }

            var currentRestartDelay = nextRestartDelayMillis;
            var restartNeeded = currentRestartDelay > 0L
                    && currentRestartDelay <= MAX_RESTART_DELAY_MILLIS
                    && !isDisposed;

            if (restartNeeded) {
                nextRestartDelayMillis = (long) ((double) currentRestartDelay * NEXT_RESTART_FACTOR);
                AppExecutorUtil.getAppScheduledExecutorService().schedule(() -> {
                            if (!isDisposed) {
                                try {
                                    DefaultAppLandJsonRpcService.this.startServer();
                                } finally {
                                    project.getMessageBus().syncPublisher(AppLandJsonRpcListener.TOPIC).serverRestarted();
                                }
                            }
                        },
                        nextRestartDelayMillis,
                        TimeUnit.MILLISECONDS);
            }
        }
    }
}
