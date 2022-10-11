package appland.execution;

import appland.AppMapBundle;
import com.intellij.execution.ExecutionException;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.configuration.ProjectSettingsService;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

public class UnsupportedJdkException extends ExecutionException implements HyperlinkListener, NotificationListener {
    private final Project project;

    public UnsupportedJdkException(@NotNull Project project) {
        super(findMessage());
        this.project = project;
    }

    @Override
    public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            ProjectSettingsService.getInstance(project).openProjectSettings();
        }
    }

    @Override
    public void hyperlinkUpdate(@NotNull Notification notification, @NotNull HyperlinkEvent event) {
        hyperlinkUpdate(event);
    }

    @NotNull
    private static String findMessage() {
        // 2021.3 and earlier don't support link handlers of ExecutionException when the exception is shown as message dialog.
        // 2022.1 and later display a notification instead of a message dialog and thus support the link handler.
        return ApplicationInfo.getInstance().getBuild().getBaselineVersion() <= 213
                ? AppMapBundle.get("appMapExecutor.jdkNotSupported")
                : AppMapBundle.get("appMapExecutor.jdkNotSupportedWithLink");
    }
}
