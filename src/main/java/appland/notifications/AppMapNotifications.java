package appland.notifications;

import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationGroupManager;
import org.jetbrains.annotations.NotNull;

public final class AppMapNotifications {
    public static final String REMOTE_RECORDING_ID = "appmap.remoteRecording";

    @NotNull
    public static NotificationGroup getRemoteRecordingGroup() {
        return NotificationGroupManager.getInstance().getNotificationGroup(REMOTE_RECORDING_ID);
    }
}
