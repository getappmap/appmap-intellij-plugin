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
@ToString
@EqualsAndHashCode
public class AppMapApplicationSettings {
    @Getter
    @Setter
    private volatile boolean appmapInstructionsViewed = false;

    @Getter
    @Setter
    private volatile boolean firstStart = true;

    @Getter
    @Setter
    private volatile boolean enableFindings = true;

    @Getter
    @Setter
    private volatile boolean enableTelemetry = true;

    @Getter
    @Setter
    private volatile @Nullable String apiKey = null;

    public AppMapApplicationSettings() {
    }

    public AppMapApplicationSettings(@NotNull AppMapApplicationSettings settings) {
        this.appmapInstructionsViewed = settings.appmapInstructionsViewed;
        this.firstStart = settings.firstStart;
        this.enableFindings = settings.enableFindings;
        this.enableTelemetry = settings.enableTelemetry;
        this.apiKey = settings.apiKey;
    }

    public void setEnableFindingsNotifying(boolean enableFindings) {
        var changed = !Objects.equals(enableFindings, this.enableFindings);
        this.enableFindings = enableFindings;

        if (changed) {
            settingsPublisher().enableFindingsChanged();
        }
    }

    public void setApiKeyNotifying(@Nullable String apiKey) {
        var changed = !Objects.equals(apiKey, this.apiKey);
        this.apiKey = apiKey;

        if (changed) {
            settingsPublisher().apiKeyChanged();
        }
    }

    public boolean isAnalysisEnabled() {
        return apiKey != null && enableFindings;
    }

    @NotNull
    private static AppMapSettingsListener settingsPublisher() {
        return ApplicationManager.getApplication().getMessageBus().syncPublisher(AppMapSettingsListener.TOPIC);
    }
}
