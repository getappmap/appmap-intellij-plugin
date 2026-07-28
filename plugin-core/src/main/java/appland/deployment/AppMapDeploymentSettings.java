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

    /**
     * Enables the AppMap scanner (runtime analysis / findings). Uses the same configuration key as the
     * VS Code plugin ({@code appMap.scannerEnabled}). {@code null} means the bundled/organization
     * configuration has no opinion, in which case the scanner stays disabled unless the user enabled it.
     */
    @SerializedName("appMap.scannerEnabled")
    @Nullable
    private Boolean scannerEnabled = null;

    public AppMapDeploymentSettings(@Nullable AppMapDeploymentTelemetrySettings telemetry) {
        this(telemetry, null, null, null, null);
    }

    public boolean isEmpty() {
        return this.telemetry == null && autoUpdateTools == null && appmapManifestUrl == null
                && scannerManifestUrl == null && scannerEnabled == null;
    }
}

