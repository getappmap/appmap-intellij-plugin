package appland.telemetry;

import appland.deployment.AppMapDeploymentSettings;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.Nullable;

public class SplunkTelemetryUtils {
    public static final String SPLUNK_BACKEND_ID = "splunk";

    public static boolean isSplunkTelemetryEnabled(@Nullable AppMapDeploymentSettings settings) {
        return settings != null && settings.getTelemetry() != null && SPLUNK_BACKEND_ID.equals(settings.getTelemetry().getBackend());
    }

    public static boolean isSplunkTelemetryActive(@Nullable AppMapDeploymentSettings settings) {
        return isSplunkTelemetryEnabled(settings)
                && settings.getTelemetry() != null
                && StringUtil.isNotEmpty(settings.getTelemetry().getUrl())
                && StringUtil.isNotEmpty(settings.getTelemetry().getToken());
    }
}
