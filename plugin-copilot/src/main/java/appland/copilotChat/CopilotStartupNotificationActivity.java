package appland.copilotChat;

import appland.ProjectActivityAdapter;
import appland.copilotChat.copilot.GitHubCopilotService;
import appland.notifications.AppMapNotifications;
import appland.settings.AppMapApplicationSettingsService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;

public class CopilotStartupNotificationActivity extends ProjectActivityAdapter implements DumbAware {
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
        GitHubCopilotService copilotService = GitHubCopilotService.getInstance();
        if (!copilotService.isCopilotAuthenticated()) {
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

        try {
            copilotService.ensureContentExclusionsDownloaded();
        } catch (Exception e) {
            // this is ok for now, it will be retried later
        }
    }
}
