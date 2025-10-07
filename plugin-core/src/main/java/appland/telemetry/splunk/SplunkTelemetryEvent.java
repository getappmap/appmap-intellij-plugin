package appland.telemetry.splunk;

import com.google.gson.annotations.SerializedName;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Refer to the VSCode plugin's SplunkTelemetryReporter.sendTelemetryEvent
 */
@Value
class SplunkTelemetryEvent {
    @SerializedName("extensionId")
    @NotNull
    String extensionId;

    @SerializedName("extensionVersion")
    @NotNull
    String extensionVersion;

    @SerializedName("eventName")
    @Nullable
    String eventName;

    @SerializedName("properties")
    @Nullable
    Map<String, String> properties;

    @SerializedName("measurements")
    @Nullable
    Map<String, Double> measurements;
}
