package appland.telemetry.appinsights;

import appland.telemetry.TelemetryEvent;
import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
class BaseData {
    @SerializedName("ver") final int version = 2;

    @SerializedName("name")
    @NotNull String name;

    @SerializedName("properties")
    @Nullable HashMap<String, String> properties;

    @SerializedName("metrics")
    @Nullable HashMap<String, Double> metrics;

    public BaseData(@NotNull String name) {
        this(name, null, null);
    }

    /**
     * Constructor to copy the properties and metrics from an existing TelemetryEvent.
     *
     * @param name  The name of the event.
     * @param commonProperties Common properties to include.
     * @param event The event to copy.
     */
    public BaseData(@NotNull String name, @Nullable Map<String, String> commonProperties, @Nullable TelemetryEvent event) {
        this.name = name;
        this.properties = new HashMap<>();
        if (commonProperties != null) {
            this.properties.putAll(commonProperties);
        }
        if (event != null) {
            this.properties.putAll(event.getProperties());
            this.metrics = new HashMap<>(event.getMetrics());
        }
    }

    public @NotNull BaseData property(String key, String value) {
        if (this.properties == null) {
            this.properties = new HashMap<>();
        }
        this.properties.put(key, value);
        return this;
    }

    public @NotNull BaseData metric(String key, Double value) {
        if (this.metrics == null) {
            this.metrics = new HashMap<>();
        }
        this.metrics.put(key, value);
        return this;
    }
}
