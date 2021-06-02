package appland.notifications;

import appland.actions.StopAppMapRecordingAction;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static appland.AppMapBundle.lazy;

public final class AppMapNotifications {
    public static final String REMOTE_RECORDING_ID = "appmap.remoteRecording";

    public static void showExpiringRecordingNotification(@NotNull Project project,
                                                         @Nullable String title,
                                                         @NotNull String content,
                                                         @NotNull NotificationType type,
                                                         boolean withClose) {

        ApplicationManager.getApplication().invokeLater(() -> {
            var notification = new Notification(
                    REMOTE_RECORDING_ID, null,
                    title, null, content,
                    type, NotificationListener.URL_OPENING_LISTENER
            );

            if (withClose) {
                notification = notification.addAction(new NotificationAction.Simple(
                        lazy("notification.closeButton"),
                        (e, n) -> n.expire(), REMOTE_RECORDING_ID));
            }

            notification.notify(project);
        }, ModalityState.any());
    }

    public static void showExpandedRecordingNotification(@NotNull Project project,
                                                         @Nullable String title,
                                                         @NotNull String content,
                                                         @NotNull NotificationType type,
                                                         boolean withClose,
                                                         boolean withStopAction) {

        ApplicationManager.getApplication().invokeLater(() -> {
            Notification notification = new AppMapFullContentNotification(
                    REMOTE_RECORDING_ID, null,
                    title, null, content,
                    type, NotificationListener.URL_OPENING_LISTENER
            );

            if (withClose) {
                var closeAction = new NotificationAction.Simple(
                        lazy("notification.closeButton"),
                        (e, n) -> n.expire(), REMOTE_RECORDING_ID);
                notification = notification.addAction(closeAction);
            }

            if (withStopAction) {
                notification = notification.addAction(new NotificationAction.Simple(lazy("notification.stopButton"),
                        (e, n) -> {
                            n.expire();
                            new StopAppMapRecordingAction().actionPerformed(e);
                        }, REMOTE_RECORDING_ID
                ));
            }

            notification.notify(project);
        }, ModalityState.any());
    }
}
