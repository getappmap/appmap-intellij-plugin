package appland.settings;

import com.google.common.collect.Maps;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.xmlb.annotations.Transient;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Persistent application state of the AppMap plugin.
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
public class AppMapApplicationSettings {
    private volatile boolean firstStart = true;
    private volatile boolean enableTelemetry = true;
    private volatile boolean enableScanner = false;
    private volatile @Nullable String apiKey = null;
    private volatile boolean useAnimation = true;
    /**
     * {@code true} if page "Install AppMap Agent" of the installation guide webview was at least shown once.
     */
    private volatile boolean installInstructionsViewed = false;
    /**
     * {@code true} if the notification should be displayed the next time an AppMap is created.
     * This flag is synced with {@link  #firstStart} to avoid the notification with existing users.
     */
    private volatile boolean showFirstAppMapNotification = false;
    /**
     * {@code true} if the warning about broken proxy settings should be displayed the next time an AppMap webview is opened.
     */
    private volatile boolean showBrokenProxyWarning = true;

    /**
     * Map of environment variables to be set when starting AppMap services
     * The key is the name of the environment variable and the value is the value of the environment variable.
     * <p>
     * We're using HashMap instead of the generic Map type here to prevent that a Map.of() value is assigned,
     * which is unsupported by the settings serializer.
     */
    private volatile HashMap<String, String> cliEnvironment = new HashMap<>();
    private volatile boolean cliPassParentEnv = true;

    /**
     * If {@code true}, the GitHub Copilot integration was explicitly disabled by the user.
     */
    private volatile boolean copilotIntegrationDisabled = false;

    /**
     * The id of the Copilot model to use with Navie.
     * A default will be used if it's null or if there's no model of the name available in Copilot.
     */
    private volatile @Nullable String copilotModelId = null;

    /**
     * Tracks if the Copilot integration was detected to be available at least once.
     */
    private volatile boolean copilotIntegrationDetected = false;

    /**
     * Maximum accepted file size in kilobytes for files pinned inside a Navie webview editor.
     */
    private volatile int maxPinnedFileSizeKB = 20;

    private volatile boolean showFailedCliDownloadError = true;

    private @Nullable volatile String selectedAppMapModel = null;

    public AppMapApplicationSettings() {
    }

    public AppMapApplicationSettings(@NotNull AppMapApplicationSettings settings) {
        this.firstStart = settings.firstStart;
        this.enableTelemetry = settings.enableTelemetry;
        this.enableScanner = settings.enableScanner;
        this.apiKey = settings.apiKey;
        this.installInstructionsViewed = settings.installInstructionsViewed;
        this.showFirstAppMapNotification = settings.showFirstAppMapNotification;
        this.cliEnvironment.putAll(settings.cliEnvironment);
        this.cliPassParentEnv = settings.cliPassParentEnv;
        this.maxPinnedFileSizeKB = settings.maxPinnedFileSizeKB;
        this.useAnimation = settings.useAnimation;
        this.showBrokenProxyWarning = settings.showBrokenProxyWarning;
        this.copilotIntegrationDisabled = settings.copilotIntegrationDisabled;
        this.copilotIntegrationDetected = settings.copilotIntegrationDetected;
        this.copilotModelId = settings.copilotModelId;
        this.showFailedCliDownloadError = settings.showFailedCliDownloadError;
        this.selectedAppMapModel = settings.selectedAppMapModel;
    }

    public @NotNull Map<String, String> getCliEnvironment() {
        // return an immutable copy to prevent callers from modifying the stored map directly.
        // We're not using Map.of() because it's unsupported by the settings deserializer.
        // Refer to com.intellij.serialization.ClassUtil.isMutableMap for supported immutable map types.
        return Collections.unmodifiableMap(cliEnvironment);
    }

    public void setCliEnvironment(@NotNull Map<String, String> environment) {
        this.cliEnvironment = new HashMap<>(environment);
    }

    @Transient
    public void setCliEnvironmentNotifying(@NotNull Map<String, String> environment) {
        var oldEnvironment = new HashMap<>(cliEnvironment);
        var newEnvironment = new HashMap<>(environment);

        this.cliEnvironment = newEnvironment;

        if (!oldEnvironment.equals(newEnvironment)) {
            var modifiedKeys = new HashSet<String>();

            var mapDifference = Maps.difference(oldEnvironment, newEnvironment);
            modifiedKeys.addAll(mapDifference.entriesOnlyOnLeft().keySet());
            modifiedKeys.addAll(mapDifference.entriesOnlyOnRight().keySet());
            modifiedKeys.addAll(mapDifference.entriesDiffering().keySet());

            if (!modifiedKeys.isEmpty()) {
                settingsPublisher().cliEnvironmentChanged(modifiedKeys);
            }
        }
    }

    public boolean hasAppMapKey() {
        return StringUtil.isNotEmpty(apiKey);
    }

    public void setApiKeyNotifying(@Nullable String apiKey) {
        var changed = !Objects.equals(apiKey, this.apiKey);
        this.apiKey = apiKey;

        if (changed) {
            settingsPublisher().apiKeyChanged();
        }
    }

    public void setEnableScannerNotifying(boolean enableScanner) {
        var changed = this.enableScanner != enableScanner;
        this.enableScanner = enableScanner;

        if (changed) {
            settingsPublisher().scannerEnabledChanged();
        }
    }

    public boolean isAuthenticated() {
        return apiKey != null;
    }

    public void setCopilotIntegrationDisabledNotifying(boolean copilotIntegrationDisabled) {
        var changed = this.copilotIntegrationDisabled != copilotIntegrationDisabled;
        this.copilotIntegrationDisabled = copilotIntegrationDisabled;

        if (changed) {
            settingsPublisher().copilotIntegrationDisabledChanged();
        }
    }

    public void setCopilotModelId(@Nullable String copilotModelId) {
        var changed = !Objects.equals(copilotModelId, this.copilotModelId);
        this.copilotModelId = copilotModelId;

        if (changed) {
            settingsPublisher().copilotModelChanged();
        }
    }

    public void setSelectedAppMapModelNotifying(@Nullable String selectedAppMapModel) {
        var changed = !Objects.equals(selectedAppMapModel, this.selectedAppMapModel);
        this.selectedAppMapModel = selectedAppMapModel;

        if (changed) {
            settingsPublisher().selectedAppMapModelChanged();
        }
    }

    @NotNull
    private static AppMapSettingsListener settingsPublisher() {
        return ApplicationManager.getApplication().getMessageBus().syncPublisher(AppMapSettingsListener.TOPIC);
    }
}
