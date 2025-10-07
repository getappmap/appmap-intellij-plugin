package appland.telemetry.splunk;

import appland.cli.AppLandCliEnvProvider;
import appland.deployment.AppMapDeploymentSettingsService;
import appland.telemetry.SplunkTelemetryUtils;
import com.intellij.openapi.util.text.StringUtil;

import java.util.Map;
import java.util.Objects;

/**
 * Adds Splunk telemetry environment variables to the CLI environment if the deployment settings indicate
 * that Splunk telemetry is enabled.
 */
public class AppMapSplunkTelemetryCliEnvProvider implements AppLandCliEnvProvider {
    @Override
    public Map<String, String> getEnvironment() {
        var deploymentSettings = AppMapDeploymentSettingsService.getCachedDeploymentSettings();
        if (SplunkTelemetryUtils.isSplunkTelemetryEnabled(deploymentSettings)) {
            var telemetry = Objects.requireNonNull(deploymentSettings.getTelemetry());

            // Splunk telemetry is turned off is there's no URL or token.
            var url = telemetry.getUrl();
            var token = telemetry.getToken();
            if (StringUtil.isNotEmpty(url) && StringUtil.isNotEmpty(token)) {
                return Map.of(
                        "APPMAP_TELEMETRY_BACKEND", "splunk",
                        "SPLUNK_URL", url,
                        "SPLUNK_TOKEN", token,
                        "SPLUNK_CA_CERT", StringUtil.defaultIfEmpty(telemetry.getCertificateAuthorityCertificate(), "")
                );
            }
        }

        return Map.of();
    }
}
