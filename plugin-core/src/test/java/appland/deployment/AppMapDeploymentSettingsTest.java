package appland.deployment;

import appland.AppMapBaseTest;
import appland.utils.GsonUtils;
import org.junit.Test;

public class AppMapDeploymentSettingsTest extends AppMapBaseTest {
    @Test
    public void jsonSerialization() {
        var settings = new AppMapDeploymentSettings(new AppMapDeploymentTelemetrySettings(
                "splunk", "https://my-splunk.example.com:443", "my-hec-token", "my-ca-cert"
        ), true);

        var expectedJson = """
                {"appMap.telemetry":{"backend":"splunk","url":"https://my-splunk.example.com:443","token":"my-hec-token","ca":"my-ca-cert"},"appMap.autoUpdateTools":true}
                """;
        assertEquals(expectedJson.trim(), GsonUtils.GSON.toJson(settings).trim());
        assertEquals(settings, GsonUtils.GSON.fromJson(expectedJson, AppMapDeploymentSettings.class));
    }

    @Test
    public void partialSettingsKeepDefaults() {
        var json = """
                {"appmap.telemetry":{"backend":"splunk","url":"https://my-splunk.example.com:443","token":"my-hec-token","ca":"my-ca-cert"}}
                """;
        var settings = GsonUtils.GSON.fromJson(json, AppMapDeploymentSettings.class);
        assertTrue("The default setting must be kept if the JSON did not define it", settings.isAutoUpdateTools());
    }

    @Test
    public void nullSettingsKeepDefaults() {
        var json = """
                {"appMap.autoUpdateTools": null}
                """;
        var settings = GsonUtils.GSON.fromJson(json, AppMapDeploymentSettings.class);
        assertTrue("The default setting must be kept if the JSON did not define it", settings.isAutoUpdateTools());
    }
}