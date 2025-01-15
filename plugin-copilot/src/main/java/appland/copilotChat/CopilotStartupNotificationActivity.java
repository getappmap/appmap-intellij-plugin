package appland.copilotChat;

import appland.copilotChat.copilot.GitHubCopilotService;
import appland.notifications.AppMapNotifications;
import appland.settings.AppMapApplicationSettingsService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;

public class CopilotStartupNotificationActivity implements StartupActivity, DumbAware {
    private static final AtomicBoolean isNotificationShown = new AtomicBoolean(false);

    @Override
    public void runActivity(@NotNull Project project) {
        // don't show if the user is not yet logged in to an AppMap account
        if (!AppMapApplicationSettingsService.getInstance().hasAppMapKey()) {
            return;
        }

        // don't show if the GitHub Copilot plugin is unavailable
        if (CopilotAppMapEnvProvider.isDisabled()) {
            return;
        }

        // if the Copilot integration is enabled, but not authenticated, show a notification
        if (!GitHubCopilotService.getInstance().isCopilotAuthenticated()) {
            ApplicationManager.getApplication().invokeLater(AppMapNotifications::showCopilotAuthenticationRequired);
            return;
        }

        // if the Copilot integration is enabled for the first time, show a notification
        var wasSeenBefore = AppMapApplicationSettingsService.getInstance().isCopilotIntegrationDetected();
        if (!wasSeenBefore) {
            if (isNotificationShown.compareAndExchange(false, true)) {
                AppMapApplicationSettingsService.getInstance().setCopilotIntegrationDetected(true);
                ApplicationManager.getApplication().invokeLater(AppMapNotifications::showFirstCopilotIntegrationEnabled);
            }
        }
    }
}
