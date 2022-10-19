package appland.settings;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

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

    public AppMapApplicationSettings() {
    }

    public AppMapApplicationSettings(@NotNull AppMapApplicationSettings settings) {
        this.appmapInstructionsViewed = settings.appmapInstructionsViewed;
        this.firstStart = settings.firstStart;
        this.enableFindings = settings.enableFindings;
        this.enableTelemetry = settings.enableTelemetry;
    }
}
