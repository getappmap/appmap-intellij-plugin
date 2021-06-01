package appland.notifications;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.notification.impl.NotificationFullContent;
import com.intellij.openapi.util.NlsContexts;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class AppMapFullContentNotification extends Notification implements NotificationFullContent {
    public AppMapFullContentNotification(@NotNull String groupId, @Nullable Icon icon, @NotNull NotificationType type) {
        super(groupId, icon, type);
    }

    public AppMapFullContentNotification(@NotNull @NonNls String groupId, @Nullable Icon icon, @Nullable @NlsContexts.NotificationTitle String title, @Nullable @NlsContexts.NotificationSubtitle String subtitle, @Nullable @NlsContexts.NotificationContent String content, @NotNull NotificationType type, @Nullable NotificationListener listener) {
        super(groupId, icon, title, subtitle, content, type, listener);
    }

    public AppMapFullContentNotification(@NotNull @NonNls String groupId, @NotNull @NlsContexts.NotificationTitle String title, @NotNull @NlsContexts.NotificationContent String content, @NotNull NotificationType type) {
        super(groupId, title, content, type);
    }

    public AppMapFullContentNotification(@NotNull @NonNls String groupId, @NotNull @NlsContexts.NotificationTitle String title, @NotNull @NlsContexts.NotificationContent String content, @NotNull NotificationType type, @Nullable NotificationListener listener) {
        super(groupId, title, content, type, listener);
    }

    public AppMapFullContentNotification(@NotNull @NonNls String groupId, @Nullable @NonNls String displayId, @NotNull @NlsContexts.NotificationTitle String title, @NotNull @NlsContexts.NotificationContent String content, @NotNull NotificationType type, @Nullable NotificationListener listener) {
        super(groupId, displayId, title, content, type, listener);
    }
}
