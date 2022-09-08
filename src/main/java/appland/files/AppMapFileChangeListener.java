package appland.files;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.util.messages.Topic;

/**
 * Listener to be notified after changes to .appmap.json files.
 */
public interface AppMapFileChangeListener {
    Topic<AppMapFileChangeListener> TOPIC = Topic.create("AppMap file change", AppMapFileChangeListener.class);

    /**
     * Posts an application-wide notification about changed appmaps.
     */
    static void sendNotification() {
        ApplicationManager.getApplication().getMessageBus()
                .syncPublisher(AppMapFileChangeListener.TOPIC)
                .refreshAppMaps();
    }

    /**
     * Sent after any refresh of the local file system
     * and after changes to .appmap.json files (e.g. created, updated, deleted, renamed).
     */
    void refreshAppMaps();
}
