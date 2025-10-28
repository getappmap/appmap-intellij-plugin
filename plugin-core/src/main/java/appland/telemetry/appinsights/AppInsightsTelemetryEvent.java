package appland.telemetry.appinsights;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.util.HashMap;

@SuppressWarnings("unused")
class AppInsightsTelemetryEvent {
    @SerializedName("name")
    @NotNull final String name = "Microsoft.ApplicationInsights.Event";

    @SerializedName("time")
    @NotNull String time;

    @SerializedName("iKey")
    @NotNull String iKey;

    @SerializedName("tags")
    @Nullable HashMap<String, String> tags;

    @SerializedName("data")
    @NotNull MessageData data;

    public AppInsightsTelemetryEvent(@NotNull String iKey, @NotNull BaseData baseData) {
        this.iKey = iKey;
        this.time = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(new java.util.Date());
        this.data = new MessageData(baseData);
    }

    public AppInsightsTelemetryEvent property(String key, String value) {
        data.baseData.property(key, value);
        return this;
    }

    public AppInsightsTelemetryEvent tag(Tag tag, String value) {
        if (tags == null) {
            tags = new HashMap<>();
        }

        tags.put(tag.getId(), value);

        return this;
    }
}
