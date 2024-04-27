package appland.settings;

import appland.notifications.AppMapNotifications;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.ProjectManager;

/**
 * Displays a notification in each of the opened projects to reload the project when the OpenAI key changes.
 */
public class OpenAIReloadProjectListener implements AppMapSettingsListener {
    @Override
    public void openAIKeyChange() {
        ReadAction.run(() -> {
            for (var project : ProjectManager.getInstance().getOpenProjects()) {
                if (!project.isDisposed() && !project.isDefault()) {
                    AppMapNotifications.showReloadProjectNotification(project);
                }
            }
        });
    }
}
