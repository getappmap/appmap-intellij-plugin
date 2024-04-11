package appland.notifications;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.notification.impl.NotificationFullContent;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Notification, which implements {@link NotificationFullContent} to show all of its content.
 */
class AppMapFullContentNotification extends Notification implements NotificationFullContent {
    @SuppressWarnings("deprecation")
    public AppMapFullContentNotification(@NotNull @NonNls String groupId,
                                         @Nullable Icon icon,
                                         @Nullable String title,
                                         @Nullable String subtitle,
                                         @NotNull String content,
                                         @NotNull NotificationType type,
                                         @Nullable NotificationListener listener) {
        super(groupId, content, type);
        setTitle(title);
        setSubtitle(subtitle);
        setIcon(icon);
        if (listener != null) {
            // There's currently no alternative to handle clickable URL links in html content.
            setListener(listener);
        }
    }
}
