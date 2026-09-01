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

    /**
     * Managed entitlement: an administrator-set customer ID which switches the plugin into its authenticated
     * state without a getappmap.com sign-in. Uses the same configuration key as the VS Code plugin
     * ({@code appMap.customerId}). Deliberately unverified — the plugin is open source, so this is a
     * business-process boundary, not a security one. Read it through {@link Entitlement}, never directly.
     */
    @SerializedName("appMap.customerId")
    @Nullable
    private String customerId;

    /**
     * Blank collapses to absent: the value is trimmed, and empty or whitespace-only reads as unset. That rule
     * lives here, in the single accessor, so nothing downstream has to remember to apply it — in particular
     * the merge of the two configuration layers, where a blank organization value has to fall through to the
     * bundled one rather than mask it.
     * <p>
     * Hand-written rather than generated: Gson populates the field directly, so normalizing in a setter or a
     * builder customization wouldn't cover the parsed configuration, which is the only way this is ever set
     * in production.
     */
    public @Nullable String getCustomerId() {
        if (customerId == null) {
            return null;
        }
        var trimmed = customerId.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public boolean isEmpty() {
        return this.telemetry == null && autoUpdateTools == null && appmapManifestUrl == null
                && scannerManifestUrl == null && scannerEnabled == null && getCustomerId() == null;
    }
}
