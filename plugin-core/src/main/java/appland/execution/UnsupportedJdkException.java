package appland.execution;

import appland.AppMapBundle;
import com.intellij.execution.ExecutionException;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.configuration.ProjectSettingsService;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

public class UnsupportedJdkException extends ExecutionException implements HyperlinkListener, NotificationListener {
    private final Project project;

    public UnsupportedJdkException(@NotNull Project project) {
        super(AppMapBundle.get("appMapExecutor.jdkNotSupported"));
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
}
