package appland.telemetry;

import appland.cli.AppLandCliEnvProvider;

import java.util.Map;

/**
 * Pass APPMAP_TELEMETRY_DISABLED in the CLI environment.
 */
public final class TelemetryStatusEnvProvider implements AppLandCliEnvProvider {
    @Override
    public Map<String, String> getEnvironment() {
        // If enabled, pass APPMAP_TELEMETRY_DISABLED=false to the AppMap CLI tools.
        return Map.of("APPMAP_TELEMETRY_DISABLED", TelemetryService.getInstance().isEnabled() ? "false" : "true");
    }
}
