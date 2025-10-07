package appland.telemetry;

import appland.deployment.AppMapDeploymentSettings;
import org.jetbrains.annotations.Nullable;

public class SplunkTelemetryUtils {
    public static final String SPLUNK_BACKEND_ID = "splunk";

    public static boolean isSplunkTelemetryEnabled(@Nullable AppMapDeploymentSettings settings) {
        return settings != null && settings.getTelemetry() != null && SPLUNK_BACKEND_ID.equals(settings.getTelemetry().getBackend());
    }
}
