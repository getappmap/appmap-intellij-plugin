package appland.deployment;

import appland.AppMapBaseTest;
import appland.utils.GsonUtils;
import org.junit.Test;

public class AppMapDeploymentSettingsTest extends AppMapBaseTest {
    @Test
    public void jsonSerialization() {
        var settings = new AppMapDeploymentSettings(new AppMapDeploymentTelemetrySettings(
                "splunk", "https://my-splunk.example.com:443", "my-hec-token", "my-ca-cert"
        ));

        var expectedJson = """
                {"appmap.telemetry":{"backend":"splunk","url":"https://my-splunk.example.com:443","token":"my-hec-token","ca":"my-ca-cert"}}
                """;
        assertEquals(expectedJson.trim(), GsonUtils.GSON.toJson(settings).trim());
        assertEquals(settings, GsonUtils.GSON.fromJson(expectedJson, AppMapDeploymentSettings.class));
    }
}