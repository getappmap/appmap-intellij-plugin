package appland.files;

import java.util.Set;

import com.intellij.util.messages.Topic;

/**
 * Listener to be notified after changes to .appmap.json files.
 */
public interface AppMapFileChangeListener {
    Topic<AppMapFileChangeListener> TOPIC = Topic.create("AppMap file change", AppMapFileChangeListener.class);

    /**
     * Sent after at least one .appmap.json file was changed (e.g. created, updated, deleted, renamed).
     * @param changeTypes       The types of changes that occurred.
     */
    void afterAppMapFileChange(Set<AppMapFileEventType> changeTypes);
}
