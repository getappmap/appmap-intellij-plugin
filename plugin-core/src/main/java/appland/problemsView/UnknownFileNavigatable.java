package appland.problemsView;

import appland.AppMapBundle;
import appland.files.AppMapFiles;
import appland.files.OpenAppMapFileNavigatable;
import appland.notifications.AppMapNotifications;
import appland.problemsView.model.ScannerFinding;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.pom.Navigatable;
import com.intellij.util.concurrency.AppExecutorUtil;
import org.jetbrains.annotations.NotNull;

/**
 * Navigatable to open an editor for the AppMap file, which belongs to a finding.
 */
final class UnknownFileNavigatable implements Navigatable {
    private final Project project;
    private final @NotNull ScannerFinding finding;

    UnknownFileNavigatable(@NotNull Project project, @NotNull ScannerFinding finding) {
        this.project = project;
        this.finding = finding;
    }

    @Override
    public boolean canNavigate() {
        return finding.getFindingsFile() != null;
    }

    @Override
    public boolean canNavigateToSource() {
        return canNavigate();
    }

    @Override
    public void navigate(boolean requestFocus) {
        var findingsFile = finding.getFindingsFile();
        if (findingsFile != null) {
            // move the lookup of the AppMap file into a background ReadAction
            ReadAction.nonBlocking(() -> AppMapFiles.findAppMapSourceFile(findingsFile))
                    .finishOnUiThread(ModalityState.current(), appMapFile -> {
                        if (appMapFile != null) {
                            new OpenAppMapFileNavigatable(project, appMapFile, finding).navigate(requestFocus);
                        } else {
                            var groupId = AppMapNotifications.GENERIC_NOTIFICATIONS_ID;
                            var title = AppMapBundle.get("notification.appMapFileNotFound.title");
                            var message = AppMapBundle.get("notification.appMapFileNotFound.message");

                            new Notification(groupId, title, message, NotificationType.WARNING).notify(project);
                        }
                    }).submit(AppExecutorUtil.getAppExecutorService());
        }
    }
}
