package appland.telemetry;

import appland.cli.AppLandCliEnvProvider;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides telemetry-related environment variables to the CLI environment,
 * such as APPMAP_TELEMETRY_DISABLED and APPMAP_TELEMETRY_PROPERTIES.
 */
public final class TelemetryStatusEnvProvider implements AppLandCliEnvProvider {
    @Override
    public Map<String, String> getEnvironment() {
        var isEnabled = TelemetryService.getInstance().isEnabled();

        var env = new HashMap<String, String>();
        env.put("APPMAP_TELEMETRY_DISABLED", isEnabled ? "false" : "true");

        if (isEnabled) {
            var properties = TelemetryProperties.create(false);
            env.put("APPMAP_TELEMETRY_PROPERTIES", TelemetryProperties.toCliJson(properties));
        }

        return env;
    }
}
