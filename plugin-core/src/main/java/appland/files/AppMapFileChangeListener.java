package appland.files;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Listener to be notified after changes to .appmap.json files.
 */
public interface AppMapFileChangeListener {
    Topic<AppMapFileChangeListener> TOPIC = Topic.create("AppMap file change", AppMapFileChangeListener.class);

    /**
     * Posts an application-wide notification about changed appmaps.
     */
    static void sendNotification(Set<AppMapFileEventType> changeTypes) {
        ApplicationManager.getApplication().getMessageBus()
                .syncPublisher(AppMapFileChangeListener.TOPIC)
                .refreshAppMaps(changeTypes);
    }

    /**
     * Sent after any refresh of the local file system
     * and after changes to .appmap.json files (e.g. created, updated, deleted, renamed).
     */
    void refreshAppMaps(@NotNull Set<AppMapFileEventType> changeTypes);
}
