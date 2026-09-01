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

    default void modelConfigChange() {
    }

    default void copilotIntegrationDisabledChanged() {
    }

    default void copilotModelChanged() {
    }

    default void scannerEnabledChanged() {
    }

    default void cliEnvironmentChanged(@NotNull Set<String> modifiedKeys) {
    }

    default void selectedAppMapModelChanged() {
    }

    default void autoUpdateToolsChanged() {
    }

    default void configurationUrlChanged() {
    }

    default void enterpriseDeploymentSettingsChanged() {
    }

    /**
     * Fired only when the effective telemetry settings actually change (not on every org-config apply).
     * Consumers that embed telemetry settings in a process environment (the JSON-RPC server and CLI
     * processes) should restart so the change takes effect; restarting is relatively expensive, so it
     * must not be triggered when telemetry is unchanged.
     */
    default void telemetrySettingsChanged() {
    }

    /**
     * Fired only when the <em>effective</em> managed customer ID changes, in either direction. Entitlement
     * makes the plugin behave as signed in, so consumers must react as they do to
     * {@link #apiKeyChanged()}: restart the services, and rebuild UI that is gated on the plugin being
     * active.
     * <p>
     * Not fired when a mutation leaves the effective value alone — clearing the organization configuration on
     * a bundled build reconverges on the bundled ID, and announcing that would bounce the CLI processes for
     * nothing.
     */
    default void customerIdChanged() {
    }
}
