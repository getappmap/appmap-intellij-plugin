package appland.actions;

import appland.Icons;
import appland.notifications.AppMapFullContentNotification;
import appland.notifications.AppMapNotifications;
import appland.remote.RemoteRecordingService;
import appland.remote.StartRemoteRecordingDialog;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAware;
import org.jetbrains.annotations.NotNull;

import static appland.AppMapBundle.get;
import static appland.AppMapBundle.lazy;

public class StartAppMapRecordingAction extends AnAction implements DumbAware {
    public StartAppMapRecordingAction() {
        super(Icons.START_RECORDING_ACTION);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        var project = e.getProject();
        assert project != null;

        var url = StartRemoteRecordingDialog.show(project);
        if (url != null) {
            var task = new Task.Backgroundable(project, get("action.startAppMapRemoteRecording.progressTitle"), false) {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    var service = RemoteRecordingService.getInstance();
                    if (service.isRecording(url)) {
                        ApplicationManager.getApplication().invokeLater(() -> {
                            var closeAction = new NotificationAction.Simple(
                                    lazy("action.startAppMapRemoteRecording.alreadyRunning.close"),
                                    (anActionEvent, notification) -> notification.expire(), this);

                            new AppMapFullContentNotification(
                                    AppMapNotifications.REMOTE_RECORDING_ID,
                                    get("action.startAppMapRemoteRecording.alreadyRunning.title"),
                                    get("action.startAppMapRemoteRecording.alreadyRunning.content", url),
                                    NotificationType.ERROR,
                                    NotificationListener.URL_OPENING_LISTENER
                            ).addAction(closeAction).notify(project);
                        });
                        return;
                    }

                    // fixme handle errors
                    // fixme show success notification?
                    service.startRecording(url);
                }
            };
            task.queue();
        }
    }
}
