package appland.settings;

import com.google.common.collect.Maps;
import com.intellij.openapi.application.ApplicationManager;
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
    private volatile @Nullable String apiKey = null;
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
     * Map of environment variables to be set when starting AppMap services
     * The key is the name of the environment variable and the value is the value of the environment variable.
     * <p>
     * We're using HashMap instead of the generic Map type here to prevent that a Map.of() value is assigned,
     * which is unsupported by the settings serializer.
     */
    private volatile HashMap<String, String> cliEnvironment = new HashMap<>();
    private volatile boolean cliPassParentEnv = true;

    public AppMapApplicationSettings() {
    }

    public AppMapApplicationSettings(@NotNull AppMapApplicationSettings settings) {
        this.firstStart = settings.firstStart;
        this.enableTelemetry = settings.enableTelemetry;
        this.apiKey = settings.apiKey;
        this.installInstructionsViewed = settings.installInstructionsViewed;
        this.showFirstAppMapNotification = settings.showFirstAppMapNotification;
        this.cliEnvironment.putAll(settings.cliEnvironment);
        this.cliPassParentEnv = settings.cliPassParentEnv;
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

    public void setApiKeyNotifying(@Nullable String apiKey) {
        var changed = !Objects.equals(apiKey, this.apiKey);
        this.apiKey = apiKey;

        if (changed) {
            settingsPublisher().apiKeyChanged();
        }
    }

    public boolean isAuthenticated() {
        return apiKey != null;
    }

    @NotNull
    private static AppMapSettingsListener settingsPublisher() {
        return ApplicationManager.getApplication().getMessageBus().syncPublisher(AppMapSettingsListener.TOPIC);
    }
}
