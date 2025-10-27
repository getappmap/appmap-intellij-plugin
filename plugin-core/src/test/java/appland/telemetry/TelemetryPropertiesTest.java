package appland.telemetry;

import appland.AppMapBaseTest;
import appland.utils.GsonUtils;
import com.google.gson.reflect.TypeToken;
import org.junit.Test;

import java.util.Map;

public class TelemetryPropertiesTest extends AppMapBaseTest {
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
