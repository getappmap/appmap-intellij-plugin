package appland.settings;

import com.intellij.openapi.application.ApplicationManager;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Persistent application state of the AppMap plugin.
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
public class AppMapApplicationSettings {
    private volatile boolean appmapInstructionsViewed = false;
    private volatile boolean firstStart = true;
    private volatile boolean enableTelemetry = true;
    private volatile @Nullable String apiKey = null;
    /**
     * {@code true} if the notification should be displayed the next time an AppMap is created.
     * This flag is synced with {@link  #firstStart} to avoid the notification with existing users.
     */
    private volatile boolean showFirstAppMapNotification = false;

    public AppMapApplicationSettings() {
    }

    public AppMapApplicationSettings(@NotNull AppMapApplicationSettings settings) {
        this.appmapInstructionsViewed = settings.appmapInstructionsViewed;
        this.firstStart = settings.firstStart;
        this.enableTelemetry = settings.enableTelemetry;
        this.apiKey = settings.apiKey;
        this.showFirstAppMapNotification = settings.showFirstAppMapNotification;
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
