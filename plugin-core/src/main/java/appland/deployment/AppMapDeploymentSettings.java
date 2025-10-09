package appland.deployment;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public final class AppMapDeploymentSettings {
    @SerializedName("appMap.telemetry")
    @Nullable
    private AppMapDeploymentTelemetrySettings telemetry;

    @SerializedName("appMap.autoUpdateTools")
    private boolean autoUpdateTools = true;

    public AppMapDeploymentSettings(@Nullable AppMapDeploymentTelemetrySettings telemetry) {
        this(telemetry, true);
    }

    public boolean isEmpty() {
        return this.telemetry == null && autoUpdateTools;
    }
}

