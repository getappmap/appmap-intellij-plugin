package appland.telemetry.splunk;

import appland.AppMapBaseTest;
import appland.AppMapDeploymentTestUtils;
import appland.deployment.AppMapDeploymentSettings;
import appland.deployment.AppMapDeploymentTelemetrySettings;
import org.junit.Test;

import java.util.Map;

public class AppMapSplunkTelemetryCliEnvProviderTest extends AppMapBaseTest {
    @Test
    public void withoutDeploymentSettings() {
        var provider = new AppMapSplunkTelemetryCliEnvProvider();
        assertEquals("Without deployment settings, the env must be empty.", 0, provider.getEnvironment().size());
    }

    @Test
    public void withoutEmptyDeploymentSettings() throws Exception {
        var provider = new AppMapSplunkTelemetryCliEnvProvider();

        AppMapDeploymentTestUtils.withSiteConfigFile(new AppMapDeploymentSettings(), () -> {
            assertEquals("With empty deployment settings, the env must be empty.", 0, provider.getEnvironment().size());
        });
    }

    @Test
    public void withoutSplunkUrl() throws Exception {
        var provider = new AppMapSplunkTelemetryCliEnvProvider();

        var splunk = new AppMapDeploymentTelemetrySettings("splunk", null, "my-token", null);
        AppMapDeploymentTestUtils.withSiteConfigFile(new AppMapDeploymentSettings(splunk), () -> {
            assertEquals("With incomplete Splunk deployment settings, the env must be empty.", 0, provider.getEnvironment().size());
        });
    }

    @Test
    public void withoutSplunkToken() throws Exception {
        var provider = new AppMapSplunkTelemetryCliEnvProvider();

        var splunk = new AppMapDeploymentTelemetrySettings("splunk", "https://my-splunk.example.com", null, null);
        AppMapDeploymentTestUtils.withSiteConfigFile(new AppMapDeploymentSettings(splunk), () -> {
            assertEquals("With incomplete Splunk deployment settings, the env must be empty.", 0, provider.getEnvironment().size());
        });
    }

    @Test
    public void withValidDeploymentSettings() throws Exception {
        var provider = new AppMapSplunkTelemetryCliEnvProvider();

        var splunk = new AppMapDeploymentTelemetrySettings("splunk", "https://my-splunk.example.com", "my-token", "my-ca-cert");
        AppMapDeploymentTestUtils.withSiteConfigFile(new AppMapDeploymentSettings(splunk), () -> {
            var expectedEnv = Map.of(
                    "APPMAP_TELEMETRY_BACKEND", "splunk",
                    "SPLUNK_URL", "https://my-splunk.example.com",
                    "SPLUNK_TOKEN", "my-token",
                    "SPLUNK_CA_CERT", "my-ca-cert");
            assertEquals(expectedEnv, provider.getEnvironment());
        });
    }
}