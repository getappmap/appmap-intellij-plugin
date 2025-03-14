package appland.settings;

import appland.AppLandLifecycleService;
import appland.notifications.AppMapNotifications;
import appland.rpcService.AppLandJsonRpcService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.util.Alarm;
import com.intellij.util.LazyInitializer;
import com.intellij.util.SingleAlarm;
import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Displays a notification in each of the opened projects to reload the project after settings changed,
 * which require a project reload to activate.
 */
public class AppMapSettingsReloadProjectListener implements AppMapSettingsListener {
    // Debounce the notification, because several separate settings may be changed at once.
    // Initialized lazily, because init references a service and other services
    // must not be accessed during initialization in 2025.1+.
    private final LazyInitializer.LazyValue<SingleAlarm> showReloadNotificationAlarm = LazyInitializer.create(() -> {
        return new SingleAlarm(
                this::showReloadNotificationInAllProjects,
                1_000,
                AppLandLifecycleService.getInstance(),
                Alarm.ThreadToUse.SWING_THREAD,
                ModalityState.defaultModalityState());
    });

    // Debounce the notification, because several separate settings may be changed at once.
    // Initialized lazily, because init references a service and other services
    // must not be accessed during initialization in 2025.1+.
    private final LazyInitializer.LazyValue<SingleAlarm> reloadJsonRpcServerAlarm = LazyInitializer.create(() -> {
        return new SingleAlarm(
                this::restartJsonRpcServerInAllProjects,
                1_000,
                AppLandLifecycleService.getInstance(),
                Alarm.ThreadToUse.POOLED_THREAD,
                ModalityState.defaultModalityState());
    });


    @Override
    public void cliEnvironmentChanged(@NotNull Set<String> modifiedKeys) {
        if (CollectionUtils.containsAny(modifiedKeys, AppLandJsonRpcService.LLM_ENV_VARIABLES)) {
            reloadJsonRpcServerAlarm.get().cancelAndRequest();
        }
    }

    @Override
    public void openAIKeyChange() {
        reloadJsonRpcServerAlarm.get().cancelAndRequest();
    }

    @Override
    public void apiKeyChanged() {
        reloadJsonRpcServerAlarm.get().cancelAndRequest();
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
    public void scannerEnabledChanged() {
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
