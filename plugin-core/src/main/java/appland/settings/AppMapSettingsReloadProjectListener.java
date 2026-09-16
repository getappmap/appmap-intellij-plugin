package appland.settings;

import appland.AppLandLifecycleService;
import appland.cli.AppLandCommandLineService;
import appland.notifications.AppMapNotifications;
import appland.rpcService.AppLandJsonRpcService;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.util.Alarm;
import com.intellij.util.LazyInitializer;
import com.intellij.util.SingleAlarm;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Displays a notification in each of the opened projects to reload the project after settings changed,
 * which require a project reload to activate.
 */
// applicationListeners subscribers must not implement Disposable (JetBrains guidelines). We still
// need alarms scoped tighter than the application: using AppLandLifecycleService directly causes
// cross-test state leaks when alarms fire after a test finishes and trigger unexpected restarts in
// the next one. The package-private constructor lets tests inject getTestRootDisposable() instead.
public class AppMapSettingsReloadProjectListener implements AppMapSettingsListener {
    // Debounce the notification, because several separate settings may be changed at once.
    // Initialized lazily, because init references a service and other services
    // must not be accessed during initialization in 2025.1+.
    private final LazyInitializer.LazyValue<SingleAlarm> showReloadNotificationAlarm;
    private final LazyInitializer.LazyValue<SingleAlarm> reloadJsonRpcServerAlarm;
    private final LazyInitializer.LazyValue<SingleAlarm> reloadCliProcessesAlarm;

    @SuppressWarnings("unused") // instantiated by the platform via XML applicationListeners registration
    public AppMapSettingsReloadProjectListener() {
        this(AppLandLifecycleService.getInstance());
    }

    @org.jetbrains.annotations.TestOnly
    @SuppressWarnings("deprecation") // SingleAlarm is deprecated in favour of Flow.debounce, which is Kotlin-only
    public AppMapSettingsReloadProjectListener(@NotNull Disposable alarmParent) {
        showReloadNotificationAlarm = LazyInitializer.create(() -> new SingleAlarm(
                this::showReloadNotificationInAllProjects,
                1_000,
                alarmParent,
                Alarm.ThreadToUse.SWING_THREAD,
                ModalityState.defaultModalityState()));
        reloadJsonRpcServerAlarm = LazyInitializer.create(() -> new SingleAlarm(
                this::restartJsonRpcServerInAllProjects,
                1_000,
                alarmParent,
                Alarm.ThreadToUse.POOLED_THREAD,
                ModalityState.defaultModalityState()));
        reloadCliProcessesAlarm = LazyInitializer.create(() -> new SingleAlarm(
                () -> AppLandCommandLineService.getInstance().restartProcessesInBackground(),
                1_000,
                alarmParent,
                Alarm.ThreadToUse.POOLED_THREAD,
                ModalityState.defaultModalityState()));
    }


    @Override
    public void cliEnvironmentChanged(@NotNull Set<String> modifiedKeys) {
        // The indexer and scanner CLI processes also receive this environment, so they must be
        // restarted alongside the JSON-RPC server for the change to take effect.
        reloadJsonRpcServerAlarm.get().cancelAndRequest();
        reloadCliProcessesAlarm.get().cancelAndRequest();
    }

    @Override
    public void modelConfigChange() {
        reloadJsonRpcServerAlarm.get().cancelAndRequest();
    }

    @Override
    public void apiKeyChanged() {
        reloadJsonRpcServerAlarm.get().cancelAndRequest();
    }

    @Override
    public void customerIdChanged() {
        // Gaining or losing entitlement starts or stops the services, exactly as signing in or out does.
        // Both alarms are needed: unlike apiKeyChanged, the CLI processes also receive APPMAP_CUSTOMER_ID,
        // and after the quiescence gate they have to start and stop with the entitlement too.
        reloadJsonRpcServerAlarm.get().cancelAndRequest();
        reloadCliProcessesAlarm.get().cancelAndRequest();
    }

    @Override
    public void copilotIntegrationDisabledChanged() {
        reloadJsonRpcServerAlarm.get().cancelAndRequest();
    }

    @Override
    public void copilotModelChanged() {
        reloadJsonRpcServerAlarm.get().cancelAndRequest();
    }

    @Override
    public void telemetrySettingsChanged() {
        // The JSON-RPC server, indexer, and scanner processes all receive telemetry settings via
        // their environment, so they must be restarted to route telemetry to the newly
        // configured backend.
        reloadJsonRpcServerAlarm.get().cancelAndRequest();
        reloadCliProcessesAlarm.get().cancelAndRequest();
    }

    @Override
    public void scannerEnabledChanged() {
        // Start or stop the scanner CLI process live: restartProcessesInBackground() re-evaluates the
        // isScannerEnabled() gate per watched root, so the scanner is launched or torn down without a
        // restart. The reload notification remains as a fallback for UI that can't be rebuilt in place
        // (the Problems View "Runtime Analysis" findings tab).
        reloadCliProcessesAlarm.get().cancelAndRequest();
        showReloadNotificationAlarm.get().cancelAndRequest();
    }

    private void showReloadNotificationInAllProjects() {
        ApplicationManager.getApplication().assertReadAccessAllowed();

        for (var project : ProjectManager.getInstance().getOpenProjects()) {
            if (!project.isDisposed() && !project.isDefault()) {
                AppMapNotifications.showReloadProjectNotification(project);
            }
        }
    }

    private void restartJsonRpcServerInAllProjects() {
        // For unknown reasons this method is executed on the EDT despite Alarm.ThreadToUse.POOLED_THREAD.
        // Seen with 2023.1.
        if (ApplicationManager.getApplication().isDispatchThread()) {
            ApplicationManager.getApplication().executeOnPooledThread(this::doRestartJsonRpcServer);
        } else {
            doRestartJsonRpcServer();
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    private void doRestartJsonRpcServer() {
        ApplicationManager.getApplication().assertIsNonDispatchThread();

        for (var project : ProjectManager.getInstance().getOpenProjects()) {
            if (!project.isDisposed() && !project.isDefault()) {
                AppLandJsonRpcService.getInstance(project).restartServer();
            }
        }
    }
}
