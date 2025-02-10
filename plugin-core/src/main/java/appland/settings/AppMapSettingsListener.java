package appland.settings;

import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

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

    default void openAIKeyChange() {
    }

    default void copilotIntegrationDisabledChanged() {
    }

    default void copilotModelChanged() {
    }

    default void scannerEnabledChanged() {
    }

    default void cliEnvironmentChanged(@NotNull Set<String> modifiedKeys) {
    }
}
