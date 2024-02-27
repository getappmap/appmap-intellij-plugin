package appland.settings;

import com.intellij.util.messages.Topic;

public interface AppMapSettingsListener {
    @Topic.AppLevel
    Topic<AppMapSettingsListener> TOPIC = Topic.create("AppMap settings change", AppMapSettingsListener.class);

    default void apiKeyChanged() {
    }

    default void createOpenApiChanged() {
    }

    default void openedAppMapChanged() {
    }

    default void investigatedFindingsChanged() {
    }

    default void explainWithNavieOpenedChanged() {
    }

    default void appMapWebViewFiltersChanged() {
    }
}
