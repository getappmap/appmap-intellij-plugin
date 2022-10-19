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

/**
 * Notification, which implements {@link NotificationFullContent} to show all of its content.
 */
class AppMapFullContentNotification extends Notification implements NotificationFullContent {
    public AppMapFullContentNotification(@NotNull @NonNls String groupId, @Nullable Icon icon, @Nullable @NlsContexts.NotificationTitle String title, @Nullable @NlsContexts.NotificationSubtitle String subtitle, @Nullable @NlsContexts.NotificationContent String content, @NotNull NotificationType type, @Nullable NotificationListener listener) {
        super(groupId, icon, title, subtitle, content, type, listener);
    }
}
