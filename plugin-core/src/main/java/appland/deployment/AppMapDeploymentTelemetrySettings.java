package appland.deployment;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public final class AppMapDeploymentTelemetrySettings {
    /**
     * Currently, only `splunk` is supported.
     */
    @SerializedName("backend")
    @Nullable String backend;

    /**
     * The URL of your Splunk HTTP Event Collector (HEC) endpoint.
     * Note it's recommended to include the port number (usually 8088 or 443).
     */
    @SerializedName("url")
    @Nullable String url;

    /**
     * Your Splunk HEC token.
     */
    @SerializedName("token")
    @Nullable String token;

    /**
     * Your CA certificate.
     * If not set, the server certificate will not be verified.
     * If set to `system`, the system's default CA certificates will be used.
     * If the value starts with `@`, it will be interpreted as a path to a CA certificate file.
     * Otherwise, the value will be used as the literal CA certificate.
     */
    @SerializedName("ca")
    @Nullable String certificateAuthorityCertificate;
}
