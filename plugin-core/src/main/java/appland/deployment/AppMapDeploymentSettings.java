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
    @Nullable
    private Boolean autoUpdateTools = null;

    @SerializedName("appMap.manifest.appmapUrl")
    @Nullable
    private String appmapManifestUrl;

    @SerializedName("appMap.manifest.scannerUrl")
    @Nullable
    private String scannerManifestUrl;

    public AppMapDeploymentSettings(@Nullable AppMapDeploymentTelemetrySettings telemetry) {
        this(telemetry, null, null, null);
    }

    public boolean isEmpty() {
        return this.telemetry == null && autoUpdateTools == null && appmapManifestUrl == null && scannerManifestUrl == null;
    }
}

