package appland.telemetry;

import appland.AppMapBaseTest;
import appland.settings.AppMapApplicationSettingsService;
import org.junit.Test;

public class TelemetryStatusEnvProviderTest extends AppMapBaseTest {
    @Test
    public void testTelemetryDisabled() {
        AppMapApplicationSettingsService.getInstance().setEnableTelemetry(false);

        var env = new TelemetryStatusEnvProvider().getEnvironment();
        assertEquals("true", env.get("APPMAP_TELEMETRY_DISABLED"));
        assertNull(env.get("APPMAP_TELEMETRY_PROPERTIES"));
    }

    @Test
    public void testTelemetryEnabled() {
        AppMapApplicationSettingsService.getInstance().setEnableTelemetry(true);

        var env = new TelemetryStatusEnvProvider().getEnvironment();
        assertEquals("false", env.get("APPMAP_TELEMETRY_DISABLED"));
        assertNotNull(env.get("APPMAP_TELEMETRY_PROPERTIES"));
    }
}
