package appland.deployment;

import appland.AppMapBaseTest;
import appland.utils.GsonUtils;
import org.junit.Test;

public class AppMapDeploymentSettingsTest extends AppMapBaseTest {
    @Test
    public void jsonSerialization() {
        var settings = AppMapDeploymentSettings.builder()
                .telemetry(new AppMapDeploymentTelemetrySettings(
                        "splunk", "https://my-splunk.example.com:443", "my-hec-token", "my-ca-cert"))
                .autoUpdateTools(true)
                .build();

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
        assertNull("The default setting must be null (absent) if the JSON did not define it", settings.getAutoUpdateTools());
    }

    @Test
    public void nullSettingsKeepDefaults() {
        var json = """
                {"appMap.autoUpdateTools": null}
                """;
        var settings = GsonUtils.GSON.fromJson(json, AppMapDeploymentSettings.class);
        assertNull("The default setting must be null (absent) if the JSON explicitly set null", settings.getAutoUpdateTools());
    }

    @Test
    public void scannerEnabledParsed() {
        var json = """
                {"appMap.scannerEnabled": true}
                """;
        var settings = GsonUtils.GSON.fromJson(json, AppMapDeploymentSettings.class);
        assertEquals("appMap.scannerEnabled must be parsed (same key as the VS Code plugin)",
                Boolean.TRUE, settings.getScannerEnabled());
    }

    @Test
    public void scannerEnabledDefaultsToNullWhenAbsent() {
        var json = """
                {"appMap.autoUpdateTools": true}
                """;
        var settings = GsonUtils.GSON.fromJson(json, AppMapDeploymentSettings.class);
        assertNull("scannerEnabled must be null (absent) when the JSON does not define it",
                settings.getScannerEnabled());
    }

    @Test
    public void customerIdParsed() {
        var json = """
                {"appMap.customerId": "acme-corp"}
                """;
        var settings = GsonUtils.GSON.fromJson(json, AppMapDeploymentSettings.class);
        assertEquals("appMap.customerId must be parsed (same key as the VS Code plugin)",
                "acme-corp", settings.getCustomerId());
    }

    @Test
    public void customerIdDefaultsToNullWhenAbsent() {
        var json = """
                {"appMap.autoUpdateTools": true}
                """;
        var settings = GsonUtils.GSON.fromJson(json, AppMapDeploymentSettings.class);
        assertNull("customerId must be null (absent) when the JSON does not define it",
                settings.getCustomerId());
    }

    @Test
    public void customerIdMakesSettingsNonEmpty() {
        assertFalse("A configuration which only sets a customer ID is not empty",
                AppMapDeploymentSettings.builder().customerId("acme-corp").build().isEmpty());
        assertTrue(new AppMapDeploymentSettings().isEmpty());
    }
}