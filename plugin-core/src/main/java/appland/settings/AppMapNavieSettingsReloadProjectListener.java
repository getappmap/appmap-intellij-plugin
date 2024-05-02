package appland.settings;

import appland.notifications.AppMapNotifications;
import appland.rpcService.AppLandJsonRpcService;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.ProjectManager;
import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Displays a notification in each of the opened projects to reload the project when the OpenAI key changes.
 */
public class AppMapNavieSettingsReloadProjectListener implements AppMapSettingsListener {
    @Override
    public void cliEnvironmentChanged(@NotNull Set<String> modifiedKeys) {
        if (CollectionUtils.containsAny(modifiedKeys, AppLandJsonRpcService.LLM_ENV_VARIABLES)) {
            showReloadNotificationInAllProjects();
        }
    }

    @Override
    public void openAIKeyChange() {
        ReadAction.run(AppMapNavieSettingsReloadProjectListener::showReloadNotificationInAllProjects);
    }

    private static void showReloadNotificationInAllProjects() {
        for (var project : ProjectManager.getInstance().getOpenProjects()) {
            if (!project.isDisposed() && !project.isDefault()) {
                AppMapNotifications.showReloadProjectNotification(project);
            }
        }
    }
}
