package appland.rpcService;

import appland.cli.AppLandCommandLineService;
import appland.cli.AppLandDownloadListener;
import appland.cli.CliTool;
import appland.config.AppMapConfigFileListener;
import appland.files.AppMapFiles;
import appland.settings.AppMapSettingsListener;
import appland.utils.GsonUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.process.KillableProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Alarm;
import com.intellij.util.SingleAlarm;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread;
import com.intellij.util.io.BaseOutputReader;
import com.intellij.util.io.HttpRequests;
import lombok.Data;
import net.jcip.annotations.GuardedBy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * This implementation restarts the JSON-RPC server up to three times if it terminates unexpectedly.
 * The following configuration changes trigger a restart:
 * - Change of the AppMap API key
 * - Download or update of an AppMap CLI binary
 * - Add/remove/update of an appmap.yml file
 */
@SuppressWarnings("UnstableApiUsage")
public class DefaultAppLandJsonRpcService implements AppLandJsonRpcService, AppLandDownloadListener, AppMapSettingsListener {
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
    private final SingleAlarm sendConfigurationAlarm = new SingleAlarm(this::sendConfigurationSet,
            1_000,
            this,
            Alarm.ThreadToUse.POOLED_THREAD);
    // debounce requests to restart the JSON-RPC server process
    private final SingleAlarm restartServerAlarm = new SingleAlarm(this::restartServerAsync,
            1_000,
            this,
            Alarm.ThreadToUse.POOLED_THREAD);

    // flag to help avoid starting the server if we're already disposed
    private volatile boolean isDisposed;
    // a value of "0" indicates that process restart is disabled
    private volatile long nextRestartDelayMillis = INITIAL_RESTART_DELAY_MILLIS;
    @GuardedBy("this")
    protected volatile @Nullable JsonRpcServer jsonRpcServer = null;

    public DefaultAppLandJsonRpcService(@NotNull Project project) {
        this.project = project;

        var application = ApplicationManager.getApplication();

        var applicationBus = application.getMessageBus().connect(this);
        applicationBus.subscribe(AppLandDownloadListener.TOPIC, this);
        applicationBus.subscribe(AppMapSettingsListener.TOPIC, this);
        applicationBus.subscribe(AppMapConfigFileListener.TOPIC, this::triggerSendConfigurationSet);

        if (!application.isUnitTestMode()) {
            // poll AppMap configuration files every 30s and send "v1.configuration.set"
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
        if (isServerRunning()) {
            LOG.debug("AppMap JSON-RPC server is already running.");
            return;
        }

        if (isDisposed || project.isDisposed()) {
            LOG.debug("Unable to start JSON-RPC server, because the service is already disposed.");
            return;
        }

        var commandLine = AppLandCommandLineService.getInstance().createAppMapJsonRpcCommand();
        if (commandLine == null) {
            LOG.debug("Unable to launch JSON-RPC server, because CLI command is unavailable.");
            return;
        }

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
    public synchronized @Nullable Integer getServerPort() {
        var server = jsonRpcServer;
        return server != null ? server.jsonRpcPort : null;
    }

    private @Nullable String getServerUrl() {
        var port = getServerPort();
        return port != null ? "http://127.0.0.1:" + port : null;
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
        try {
            stopServerSync(this.jsonRpcServer);
        } finally {
            startServer();
        }
    }

    /**
     * Stops the server and waits for a short time before it's forcibly killed.
     *
     * @param server The server to stop, a {@code null} value is ignored.
     */
    @RequiresBackgroundThread
    private void stopServerSync(@Nullable JsonRpcServer server) {
        ApplicationManager.getApplication().assertIsNonDispatchThread();

        if (server == null) {
            return;
        }

        try {
            var process = server.processHandler;
            process.destroyProcess();
            process.waitFor(500);
            if (!process.isProcessTerminated()) {
                process.killProcess();
            }
        } finally {
            if (!isDisposed) {
                project.getMessageBus().syncPublisher(AppLandJsonRpcListener.TOPIC).serverStopped();
            }
        }
    }

    @Override
    public void downloadFinished(@NotNull CliTool type, boolean success) {
        if (success && CliTool.AppMap.equals(type) && !isServerRunning()) {
            restartServerAsync();
        }
    }

    @Override
    public void apiKeyChanged() {
        triggerRestartServer();
    }

    private void triggerSendConfigurationSet() {
        sendConfigurationAlarm.cancelAndRequest();
    }

    private void triggerRestartServer() {
        restartServerAlarm.cancelAndRequest();
    }

    /**
     * Sends message "v1.configuration.set" to the currently running JSON-RPC server.
     */
    private void sendConfigurationSet() {
        ApplicationManager.getApplication().assertIsNonDispatchThread();

        var serverUrl = getServerUrl();
        if (serverUrl == null) {
            LOG.debug("Unable to send \"v1.configuration.set\" because JSON-RPC service is unavailable.");
            return;
        }

        var configFiles = DumbService.getInstance(project).runReadActionInSmartMode(() -> {
            return AppMapFiles.findAppMapConfigFiles(project);
        });

        var jsonConfigFiles = new JsonArray();
        configFiles.stream()
                .map(file -> {
                    var path = file.getFileSystem().getNioPath(file);
                    return path != null ? path.normalize().toString() : null;
                })
                .filter(Objects::nonNull)
                .forEach(jsonConfigFiles::add);

        try {
            var payload = new JsonObject();
            payload.addProperty("jsonrpc", "2.0");
            payload.addProperty("method", "v1.configuration.set");
            payload.add("params", GsonUtils.singlePropertyObject("appmapConfigFiles", jsonConfigFiles));

            var json = GsonUtils.GSON.toJson(payload);
            if (LOG.isTraceEnabled()) {
                LOG.trace("Sending JSON-RPC message: " + json);
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug("Sending request v1.configuration.set. Request payload: " + json);
            }

            HttpRequests.post(serverUrl, HttpRequests.JSON_CONTENT_TYPE).write(json);
        } catch (IOException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Error sending request v1.configuration.set", e);
            }
        } finally {
            if (!isDisposed) {
                project.getMessageBus().syncPublisher(AppLandJsonRpcListener.TOPIC).serverConfigurationUpdated(configFiles);
            }
        }
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
            if (LOG.isDebugEnabled() && outputType != ProcessOutputTypes.SYSTEM) {
                LOG.debug("JSON-RPC: " + event.getText().trim());
            }

            var match = PORT_PATTERN.matcher(event.getText().trim());
            if (match.matches()) {
                var port = StringUtil.parseInt(match.group(1), -1);
                if (port > 0) {
                    jsonRpcServer.jsonRpcPort = port;
                    triggerSendConfigurationSet();
                }
            }
        }

        @Override
        public void processTerminated(@NotNull ProcessEvent event) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("AppMap JSON-RPC server terminated: " + event);
            }

            synchronized (DefaultAppLandJsonRpcService.this) {
                DefaultAppLandJsonRpcService.this.jsonRpcServer = null;
                jsonRpcServer.jsonRpcPort = null;
            }

            var currentRestartDelay = nextRestartDelayMillis;
            var restartNeeded = currentRestartDelay >= 0L
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
