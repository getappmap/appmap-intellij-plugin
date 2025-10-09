package appland.cli;

import appland.notifications.AppMapNotifications;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public final class FailedDownloadNotificationListener implements AppLandDownloadListener {
    @NotNull private final Project project;

    public FailedDownloadNotificationListener(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public void downloadFinished(@NotNull CliTool type, @NotNull AppMapDownloadStatus status) {
        if (!status.isSuccessful()) {
            AppMapNotifications.showFailedCliBinaryDownloadNotification(project);
        }
    }
}
