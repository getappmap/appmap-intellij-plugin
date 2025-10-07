package appland.telemetry;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Mutable event data to help creating telemetry events.
 * This class is not thread-safe.
 */
public class TelemetryEvent {
    private @Nullable String name;
    private @Nullable HashMap<String, String> properties;
    private @Nullable HashMap<String, Double> metrics;

    public TelemetryEvent() {
    }

    public TelemetryEvent(@Nullable String name) {
        this.name = name;
    }

    public @Nullable String getName() {
        return name;
    }

    /**
     * @return Immutable copy of the properties. This map may be empty.
     */
    public @NotNull Map<String, String> getProperties() {
        return properties == null ? Map.of() : Map.copyOf(properties);
    }

    /**
     * @return Immutable copy of the metrics. This map may be empty.
     */
    public @NotNull Map<String, Double> getMetrics() {
        return metrics == null ? Map.of() : Map.copyOf(metrics);
    }

    public @NotNull TelemetryEvent withName(@NotNull String name) {
        this.name = name;
        return this;
    }

    public @NotNull TelemetryEvent withProperty(@NotNull String key, @NotNull String value) {
        if (this.properties == null) {
            this.properties = new HashMap<>();
        }
        this.properties.put(key, value);
        return this;
    }

    public @NotNull TelemetryEvent withMetric(@NotNull String key, double value) {
        if (this.metrics == null) {
            this.metrics = new HashMap<>();
        }
        this.metrics.put(key, value);
        return this;
    }
}
