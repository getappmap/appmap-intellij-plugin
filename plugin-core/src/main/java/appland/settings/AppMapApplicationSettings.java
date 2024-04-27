package appland.settings;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.application.ApplicationManager;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.intellij.openapi.util.text.StringUtil.isNotEmpty;

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
     */
    private volatile Map<String, String> cliEnvironment = new HashMap<>();
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

    public void setCliEnvironment(@NotNull Map<String, String> environment) {
        this.cliEnvironment = Map.copyOf(environment);
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
