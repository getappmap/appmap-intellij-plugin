package appland.telemetry.splunk;

import lombok.Data;
import org.jetbrains.annotations.Nullable;

@Data
public class SplunkSettings {
    /**
     * The URL of your Splunk HTTP Event Collector (HEC) endpoint.
     * Note it's recommended to include the port number (usually 8088 or 443).
     */
    private final @Nullable String url;
    /**
     * Your Splunk HEC token.
     */
    private final @Nullable String token;
    /**
     * Your CA certificate.
     * If not set, the server certificate will not be verified.
     * If set to `system`, the system's default CA certificates will be used.
     * If the value starts with `@`, it will be interpreted as a path to a CA certificate file.
     * Otherwise, the value will be used as the literal CA certificate.
     */
    private final @Nullable String ca;
}
