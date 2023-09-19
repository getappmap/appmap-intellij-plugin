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
@ToString
@EqualsAndHashCode
public class AppMapApplicationSettings {
    @Setter
    private volatile boolean appmapInstructionsViewed = false;

    @Setter
    private volatile boolean firstStart = true;

    @Setter
    private volatile boolean enableTelemetry = true;

    @Setter
    private volatile @Nullable String apiKey = null;

    public AppMapApplicationSettings() {
    }

    public AppMapApplicationSettings(@NotNull AppMapApplicationSettings settings) {
        this.appmapInstructionsViewed = settings.appmapInstructionsViewed;
        this.firstStart = settings.firstStart;
        this.enableTelemetry = settings.enableTelemetry;
        this.apiKey = settings.apiKey;
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
