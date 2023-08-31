package appland.config;

import com.intellij.util.messages.Topic;

/**
 * Listener for changes to appmap.yaml files.
 */
@FunctionalInterface
public interface AppMapConfigFileListener {
    @Topic.AppLevel
    Topic<AppMapConfigFileListener> TOPIC = Topic.create("AppMap config file change", AppMapConfigFileListener.class);

    void refreshAppMapConfigs();
}
