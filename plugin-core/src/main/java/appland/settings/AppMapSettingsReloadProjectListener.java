package appland.settings;

import appland.AppLandLifecycleService;
import appland.notifications.AppMapNotifications;
import appland.rpcService.AppLandJsonRpcService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.util.Alarm;
import com.intellij.util.SingleAlarm;
import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Displays a notification in each of the opened projects to reload the project after settings changed,
 * which require a project reload to activate.
 */
public class AppMapSettingsReloadProjectListener implements AppMapSettingsListener {
    @Override
    public void cliEnvironmentChanged(@NotNull Set<String> modifiedKeys) {
        if (CollectionUtils.containsAny(modifiedKeys, AppLandJsonRpcService.LLM_ENV_VARIABLES)) {
            showReloadNotificationAlarm.cancelAndRequest();
        }
    }

    @Override
    public void openAIKeyChange() {
        showReloadNotificationAlarm.cancelAndRequest();
    }

    @Override
    public void copilotIntegrationDisabledChanged() {
        showReloadNotificationAlarm.cancelAndRequest();
    }

    @Override
    public void scannedEnabledChanged() {
        showReloadNotificationInAllProjects();
    }

    // debounce the notification, because several separate settings are changed at once
    private static final SingleAlarm showReloadNotificationAlarm = new SingleAlarm(
            AppMapSettingsReloadProjectListener::showReloadNotificationInAllProjects,
            1_000,
            AppLandLifecycleService.getInstance(),
            Alarm.ThreadToUse.SWING_THREAD,
            ModalityState.defaultModalityState());

    private static void showReloadNotificationInAllProjects() {
        ApplicationManager.getApplication().assertReadAccessAllowed();

        for (var project : ProjectManager.getInstance().getOpenProjects()) {
            if (!project.isDisposed() && !project.isDefault()) {
                AppMapNotifications.showReloadProjectNotification(project);
            }
        }
    }
}
