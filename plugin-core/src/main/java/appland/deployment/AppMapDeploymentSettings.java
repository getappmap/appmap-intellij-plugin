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
    @SerializedName("appmap.telemetry")
    @Nullable
    AppMapDeploymentTelemetrySettings telemetry;
}

