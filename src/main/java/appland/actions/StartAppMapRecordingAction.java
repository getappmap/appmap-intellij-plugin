package appland.actions;

import appland.Icons;
import appland.notifications.AppMapNotifications;
import appland.remote.RemoteRecordingService;
import appland.remote.RemoteRecordingStatusService;
import appland.remote.StartRemoteRecordingDialog;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAware;
import org.jetbrains.annotations.NotNull;

import static appland.AppMapBundle.get;

public class StartAppMapRecordingAction extends AnAction implements DumbAware {
    public StartAppMapRecordingAction() {
        super(Icons.START_RECORDING_ACTION);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        var project = e.getProject();
        if (project == null) {
            return;
        }

        // only hide in the AppMap tool window's toolbar, still show it in the global ist of actions
        if (e.isFromActionToolbar()) {
            var recording = RemoteRecordingStatusService.getInstance(project).getActiveRecordingURL() != null;
            e.getPresentation().setEnabledAndVisible(!recording);
        }
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        var project = e.getProject();
        assert project != null;

        var activeRecordingURL = RemoteRecordingStatusService.getInstance(project).getActiveRecordingURL();
        if (activeRecordingURL != null) {
            AppMapNotifications.showExpandedRecordingNotification(project,
                    get("notification.alreadyRecording.title"),
                    get("notification.alreadyRecording.content", activeRecordingURL),
                    NotificationType.ERROR, true, true);
            return;
        }

        var url = StartRemoteRecordingDialog.show(project);
        if (url == null) {
            return;
        }

        new Task.Backgroundable(project, get("action.startAppMapRemoteRecording.progressTitle"), false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                if (RemoteRecordingService.getInstance().isRecording(url)) {
                    AppMapNotifications.showExpandedRecordingNotification(project,
                            get("notification.alreadyRecording.title"),
                            get("notification.alreadyRecording.content", url),
                            NotificationType.ERROR, true, true);
                    return;
                }

                var success = RemoteRecordingService.getInstance().startRecording(url);
                if (success) {
                    AppMapNotifications.showExpiringRecordingNotification(project, null,
                            get("notification.recordingStarted.content", url),
                            NotificationType.INFORMATION, false);

                    RemoteRecordingStatusService.getInstance(project).recordingStarted(url);
                } else {
                    AppMapNotifications.showExpandedRecordingNotification(project,
                            get("notification.recordingStartFailed.title"),
                            get("notification.recordingStartFailed.content", url),
                            NotificationType.ERROR, true, false);
                }
            }
        }.queue();
    }
}
