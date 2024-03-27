package appland.rpcService;

import appland.cli.AppLandCommandLineService;
import appland.cli.AppLandDownloadListener;
import appland.cli.CliTool;
import appland.config.AppMapConfigFileListener;
import appland.files.AppMapFiles;
import appland.utils.GsonUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.process.KillableProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Alarm;
import com.intellij.util.SingleAlarm;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.io.BaseOutputReader;
import com.intellij.util.io.HttpRequests;
import lombok.Data;
import net.jcip.annotations.GuardedBy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * This implementation restarts the JSON-RPC server up to three times if it terminates unexpectedly.
 */
public class DefaultAppLandJsonRpcService implements AppLandJsonRpcService, AppLandDownloadListener {
    private static final Logger LOG = Logger.getInstance(DefaultAppLandJsonRpcService.class);
    private static final Pattern PORT_PATTERN = Pattern.compile("^Running JSON-RPC server on port: (\\d+)$");

    // initial delay for the first restart attempt
    private static final long INITIAL_RESTART_DELAY_MILLIS = 5_000;
    // factor to calculate the next restart delay based on the previous delay
    private static final double NEXT_RESTART_FACTOR = 1.5;
    // three restarts at most (5_000 * 1.5^3 > 15_000)
    private static final long MAX_RESTART_DELAY_MILLIS = INITIAL_RESTART_DELAY_MILLIS * 3;

    private final @NotNull Project project;
    private final @Nullable ScheduledFuture<?> pollingFuture;
    // a value of "0" indicates that process restart is disabled
    private long nextRestartDelay = INITIAL_RESTART_DELAY_MILLIS;
    // debounce requests to send the configuration message to the server
    private final SingleAlarm sendConfigurationAlarm = new SingleAlarm(this::sendConfigurationSet,
            1_000,
            this,
            Alarm.ThreadToUse.POOLED_THREAD);

    @GuardedBy("this")
    protected volatile @Nullable JsonRpcServer jsonRpcServer = null;

    public DefaultAppLandJsonRpcService(@NotNull Project project) {
        this.project = project;

        var messageBus = project.getMessageBus().connect(this);
        messageBus.subscribe(AppLandDownloadListener.TOPIC, this);
        messageBus.subscribe(AppMapConfigFileListener.TOPIC, (AppMapConfigFileListener) this::triggerSendConfigurationSet);

        if (!ApplicationManager.getApplication().isUnitTestMode()) {
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
        this.nextRestartDelay = 0L;

        try {
            var future = pollingFuture;
            if (future != null) {
                future.cancel(true);
            }
        } finally {
            stopServer();
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
                project.getMessageBus().syncPublisher(AppLandJsonRpcListener.TOPIC).serverStarted();
            }
        }
    }

    @Override
    public synchronized void stopServer() {
        // terminate in background
        var server = jsonRpcServer;
        if (server != null) {
            this.jsonRpcServer = null;

            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                try {
                    var process = server.processHandler;
                    process.destroyProcess();
                    process.waitFor(500);
                    if (!process.isProcessTerminated()) {
                        process.killProcess();
                    }
                } finally {
                    if (!project.isDisposed()) {
                        project.getMessageBus().syncPublisher(AppLandJsonRpcListener.TOPIC).serverStopped();
                    }
                }
            });
        }
    }

    @Override
    public synchronized @Nullable Integer getServerPort() {
        var server = jsonRpcServer;
        return server != null ? server.jsonRpcPort : null;
    }

    @Override
    public void downloadFinished(@NotNull CliTool type, boolean success) {
        if (success && CliTool.AppMap.equals(type) && !isServerRunning()) {
            try {
                stopServer();
            } finally {
                startServer();
            }
        }
    }

    private void triggerSendConfigurationSet() {
        sendConfigurationAlarm.cancelAndRequest();
    }

    /**
     * Sends message "v1.configuration.set" to the currently running JSON-RPC server.
     */
    private void sendConfigurationSet() {
        ApplicationManager.getApplication().assertIsNonDispatchThread();

        var port = getServerPort();
        if (port == null) {
            LOG.debug("Unable to send \"v1.configuration.set\" because JSON-RPC service is unavailable.");
            return;
        }

        var appMapConfigFiles = DumbService.getInstance(project).runReadActionInSmartMode(() -> {
            return AppMapFiles.findAppMapConfigFiles(project);
        });

        var jsonConfigFiles = new JsonArray();
        for (var file : ReadAction.compute(() -> appMapConfigFiles)) {
            var nioPath = file.getFileSystem().getNioPath(file);
            if (nioPath != null) {
                jsonConfigFiles.add(nioPath.normalize().toString());
            }
        }

        try {
            var payload = new JsonObject();
            payload.addProperty("jsonrpc", "2.0");
            payload.addProperty("method", "v1.configuration.set");
            payload.add("params", GsonUtils.singlePropertyObject("appmapConfigFiles", jsonConfigFiles));

            var body = GsonUtils.GSON.toJson(payload);
            HttpRequests.post("http://127.0.0.1:" + port, body);
        } finally {
            project.getMessageBus().syncPublisher(AppLandJsonRpcListener.TOPIC).serverConfigurationUpdated(appMapConfigFiles);
        }
    }

    @Data
    protected static class JsonRpcServer {
        @NotNull KillableProcessHandler processHandler;
        @Nullable Integer jsonRpcPort = null;
    }

    private class JsonRpcProcessListener extends ProcessAdapter {
        private final JsonRpcServer jsonRpcServer;

        public JsonRpcProcessListener(JsonRpcServer jsonRpcServer) {
            this.jsonRpcServer = jsonRpcServer;
        }

        @Override
        public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
            if (outputType != ProcessOutputTypes.SYSTEM && LOG.isDebugEnabled()) {
                LOG.debug("JSON-RPC: " + event.getText());
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

            var currentRestartDelay = nextRestartDelay;
            var restartNeeded = currentRestartDelay >= 0L && currentRestartDelay <= MAX_RESTART_DELAY_MILLIS;
            if (restartNeeded) {
                nextRestartDelay = (long) ((double) currentRestartDelay * NEXT_RESTART_FACTOR);
                AppExecutorUtil.getAppScheduledExecutorService().schedule(() -> {
                            try {
                                DefaultAppLandJsonRpcService.this.startServer();
                            } finally {
                                project.getMessageBus().syncPublisher(AppLandJsonRpcListener.TOPIC).serverRestarted();
                            }
                        },
                        nextRestartDelay,
                        TimeUnit.MILLISECONDS);
            }
        }
    }
}
