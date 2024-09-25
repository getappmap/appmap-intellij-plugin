package appland.settings;

import appland.AppMapBaseTest;
import com.intellij.configurationStore.XmlSerializer;
import com.intellij.openapi.util.JDOMUtil;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

public class AppMapApplicationSettingsTest extends AppMapBaseTest {
    @Test
    public void xmlSerialization() {
        var settings = createSettings();

        var serialized = XmlSerializer.serialize(settings);
        Assert.assertNotNull(serialized);

        var deserialized = XmlSerializer.deserialize(serialized, AppMapApplicationSettings.class);
        Assert.assertEquals(settings, deserialized);

        var expectedXML = """
                <AppMapApplicationSettings>
                  <option name="apiKey" value="my-appmap-api-key" />
                  <option name="cliEnvironment">
                    <map>
                      <entry key="name1" value="value1" />
                      <entry key="name2" value="value2" />
                    </map>
                  </option>
                  <option name="installInstructionsViewed" value="true" />
                  <option name="maxPinnedFileSizeKB" value="40" />
                </AppMapApplicationSettings>""";
        Assert.assertEquals(expectedXML, JDOMUtil.write(serialized));
    }

    @Test
    public void copy() {
        var settings = createSettings();
        var copiedSettings = new AppMapApplicationSettings(settings);
        Assert.assertEquals("Copy constructor must copy all settings", settings, copiedSettings);
    }

    @NotNull
    private static AppMapApplicationSettings createSettings() {
        var settings = new AppMapApplicationSettings();
        settings.setCliEnvironmentNotifying(Map.of("name1", "value1", "name2", "value2"));
        settings.setApiKey("my-appmap-api-key");
        settings.setInstallInstructionsViewed(true);
        settings.setFirstStart(true);
        settings.setCliPassParentEnv(true);
        settings.setMaxPinnedFileSizeKB(40);
        return settings;
    }
}