package appland.actions;

import appland.Icons;
import appland.notifications.AppMapNotifications;
import appland.remote.RemoteRecordingService;
import appland.remote.RemoteRecordingStatusService;
import appland.remote.StartRemoteRecordingDialog;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAware;
import org.jetbrains.annotations.NotNull;

import static appland.AppMapBundle.get;

/**
 * Part of the AppMap feature surface, but deliberately not an {@link AppMapFeatureAction}: it also controls
 * its own visibility in the AppMap tool window's toolbar, which isn't expressible as a predicate.
 */
public class StartAppMapRecordingAction extends AnAction implements DumbAware {
    public StartAppMapRecordingAction() {
        super(Icons.START_RECORDING_ACTION);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        var project = e.getProject();
        if (project == null) {
            return;
        }

        // Apply the feature-surface gate HERE, and not inside the isFromActionToolbar() branch below:
        // the branch is skipped for the Tools > AppMap menu, so a gate placed inside it would leave the
        // menu entry enabled while the plugin is inactive. AppMapActionAuthenticationGateTest covers this.
        if (!AppMapFeatureAction.isAppMapAvailable()) {
            e.getPresentation().setEnabled(false);
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
                    get("notification.alreadyRecording.content"),
                    NotificationType.ERROR, true, true, false);
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
                            NotificationType.ERROR, true, true, false);
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
                            NotificationType.ERROR, true, false, true);
                }
            }
        }.queue();
    }
}
