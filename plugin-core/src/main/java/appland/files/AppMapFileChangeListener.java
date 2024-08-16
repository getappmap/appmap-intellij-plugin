package appland.files;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Listener to be notified after changes to .appmap.json files.
 */
@FunctionalInterface
public interface AppMapFileChangeListener {
    Topic<AppMapFileChangeListener> TOPIC = Topic.create("AppMap file change", AppMapFileChangeListener.class);

    /**
     * Posts an application-wide notification about changed AppMaps.
     */
    static void sendNotification(@NotNull Set<AppMapFileEventType> changeTypes, boolean isGenericRefresh) {
        ApplicationManager.getApplication().getMessageBus()
                .syncPublisher(AppMapFileChangeListener.TOPIC)
                .refreshAppMaps(changeTypes, isGenericRefresh);
    }

    /**
     * Sent after any refresh of the local file system
     * and after changes to .appmap.json files (e.g. created, updated, deleted, renamed).
     *
     * @param isGenericRefresh {@code true} if the refresh was triggered by a generic refresh of the filesystem
     *                         when it's not know that an .appmap.json file was changed.
     */
    void refreshAppMaps(@NotNull Set<AppMapFileEventType> changeTypes, boolean isGenericRefresh);
}
