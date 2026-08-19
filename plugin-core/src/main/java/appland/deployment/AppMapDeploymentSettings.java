package appland.deployment;

import com.google.gson.annotations.SerializedName;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

/**
 * Construct with {@link #builder()}. The all-args constructor is private on purpose: these fields are all
 * nullable and mostly of the same few types, so a positional call silently accepts a wrong ordering, and
 * every added field breaks every call site.
 */
@Data
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public final class AppMapDeploymentSettings {
    @SerializedName("appMap.telemetry")
    @Nullable
    private AppMapDeploymentTelemetrySettings telemetry;

    @SerializedName("appMap.autoUpdateTools")
    @Nullable
    private Boolean autoUpdateTools;

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
    private Boolean scannerEnabled;

    public boolean isEmpty() {
        return this.telemetry == null && autoUpdateTools == null && appmapManifestUrl == null
                && scannerManifestUrl == null && scannerEnabled == null;
    }
}
