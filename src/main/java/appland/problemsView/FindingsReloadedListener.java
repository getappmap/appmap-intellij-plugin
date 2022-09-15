package appland.problemsView;

import com.intellij.util.messages.Topic;

/**
 * Listener to notify when findings are loaded for the first time or reloaded later.
 */
public interface FindingsReloadedListener {
    Topic<FindingsReloadedListener> TOPIC = Topic.create("AppMap findings reloaded", FindingsReloadedListener.class);

    void afterFindingsReloaded();
}
