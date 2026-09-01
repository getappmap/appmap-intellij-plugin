package appland.telemetry;

import appland.AppMapBaseTest;
import appland.deployment.AppMapDeploymentSettings;
import appland.deployment.AppMapDeploymentSettingsService;
import appland.utils.GsonUtils;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.junit.After;
import org.junit.Test;

import java.util.Map;
import java.util.Set;

public class TelemetryPropertiesTest extends AppMapBaseTest {
    @After
    public void resetDeploymentSettings() {
        AppMapDeploymentSettingsService.reset();
    }

    @Test
    public void testCreateProperties() {
        var properties = TelemetryProperties.create(false);
        assertCommonProperties(properties);
        assertNull(properties.get(TelemetryProperties.USERNAME));

        var propertiesWithUsername = TelemetryProperties.create(true);
        assertCommonProperties(propertiesWithUsername);
        assertNotNull(propertiesWithUsername.get(TelemetryProperties.USERNAME));
    }

    @Test
    public void testToCliJson() {
        var properties = TelemetryProperties.create(false);
        var json = TelemetryProperties.toCliJson(properties);

        Map<String, String> deserialized = GsonUtils.GSON.fromJson(json, new TypeToken<Map<String, String>>() {
        }.getType());

        assertEquals(properties.get(TelemetryProperties.EXT_NAME), deserialized.get(TelemetryProperties.EXT_NAME));
        assertEquals(properties.get(TelemetryProperties.EXT_VERSION), deserialized.get(TelemetryProperties.EXT_VERSION));
        assertEquals(properties.get(TelemetryProperties.IDE), deserialized.get(TelemetryProperties.IDE));
        assertEquals(properties.get(TelemetryProperties.IDE_VERSION), deserialized.get(TelemetryProperties.IDE_VERSION));
    }

    @Test
    public void testWithCommonPrefix() {
        var properties = TelemetryProperties.create(false);
        var prefixed = TelemetryProperties.withCommonPrefix(properties);

        for (var entry : properties.entrySet()) {
            assertEquals(entry.getValue(), prefixed.get("common." + entry.getKey()));
        }

        assertThrows(UnsupportedOperationException.class, () -> prefixed.put("a", "b"));
    }

    // --- the managed customer ID is attached to every event, so seat usage can be attributed ---

    @Test
    public void customerIdIncludedWhenEntitled() {
        entitle();

        assertEquals("acme-corp", TelemetryProperties.create(false).get(TelemetryProperties.CUSTOMER_ID));
    }

    @Test
    public void customerIdOmittedWhenNotEntitled() {
        assertFalse("The property must be absent entirely, not present and empty",
                TelemetryProperties.create(false).containsKey(TelemetryProperties.CUSTOMER_ID));
    }

    @Test
    public void customerIdGetsTheCommonPrefix() {
        entitle();

        var prefixed = TelemetryProperties.withCommonPrefix(TelemetryProperties.create(false));
        assertEquals("Both reporters take the prefixed set, so neither backend needs to know about the key",
                "acme-corp", prefixed.get("common.customerid"));
    }

    /**
     * {@code CliProperties} cherry-picks the four keys the CLI needs. The CLI learns the customer ID from
     * {@code APPMAP_CUSTOMER_ID} and stamps its own telemetry, so it must not also arrive here.
     */
    @Test
    public void customerIdIsNotSentToTheCli() {
        entitle();

        var json = GsonUtils.GSON.fromJson(
                TelemetryProperties.toCliJson(TelemetryProperties.create(false)), JsonObject.class);

        assertFalse("The customer ID must not reach the CLI twice: " + json,
                json.has(TelemetryProperties.CUSTOMER_ID));
        assertEquals("APPMAP_TELEMETRY_PROPERTIES carries exactly the keys the CLI needs",
                Set.of(TelemetryProperties.EXT_NAME,
                        TelemetryProperties.EXT_VERSION,
                        TelemetryProperties.IDE,
                        TelemetryProperties.IDE_VERSION),
                json.keySet());
    }

    private static void entitle() {
        AppMapDeploymentSettingsService.getInstance().setEnterpriseDeploymentSettings(
                AppMapDeploymentSettings.builder().customerId("acme-corp").build());
    }

    private void assertCommonProperties(Map<String, String> properties) {
        assertNotNull(properties.get(TelemetryProperties.EXT_NAME));
        assertNotNull(properties.get(TelemetryProperties.EXT_VERSION));
        assertNotNull(properties.get(TelemetryProperties.IDE));
        assertNotNull(properties.get(TelemetryProperties.IDE_VERSION));
        assertNotNull(properties.get(TelemetryProperties.OS));
        assertNotNull(properties.get(TelemetryProperties.OS_VERSION));
        assertNotNull(properties.get(TelemetryProperties.JVM_VERSION));
        assertNotNull(properties.get(TelemetryProperties.PRODUCT));
        assertNotNull(properties.get(TelemetryProperties.SOURCE));
    }
}
