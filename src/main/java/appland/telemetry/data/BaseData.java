package appland.telemetry.data;

import java.util.HashMap;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.gson.annotations.SerializedName;

public class BaseData {
    @SerializedName("ver")
    @NotNull final Integer version = 2;

    @SerializedName("name")
    @NotNull String name;

    @SerializedName("properties")
    @Nullable HashMap<String, String> properties;

    @SerializedName("metrics")
    @Nullable HashMap<String, Double> metrics;

    public BaseData(@NotNull String name) {
        this.name = name;
    }

    public BaseData property(String key, String value) {
        if (this.properties == null) {
            this.properties = new HashMap<>();
        }
        this.properties.put(key, value);
        return this;
    }

    public BaseData metric(String key, Double value) {
        if (this.metrics == null) {
            this.metrics = new HashMap<>();
        }
        this.metrics.put(key, value);
        return this;
    }
}
