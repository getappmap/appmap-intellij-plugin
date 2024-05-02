package appland.settings;

import appland.AppMapBaseTest;
import com.intellij.configurationStore.XmlSerializer;
import com.intellij.openapi.util.JDOMUtil;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

public class AppMapApplicationSettingsTest extends AppMapBaseTest {
    @Test
    public void xmlSerialization() {
        var settings = new AppMapApplicationSettings();
        settings.setCliEnvironmentNotifying(Map.of("name1", "value1", "name2", "value2"));
        settings.setApiKey("my-appmap-api-key");
        settings.setInstallInstructionsViewed(true);
        settings.setFirstStart(true);
        settings.setCliPassParentEnv(true);

        var serialized = XmlSerializer.serialize(settings);
        Assert.assertNotNull(serialized);

        var deserialized = XmlSerializer.deserialize(serialized, AppMapApplicationSettings.class);
        Assert.assertEquals(settings, deserialized);

        var expectedXML = "<AppMapApplicationSettings>\n" +
                "  <option name=\"apiKey\" value=\"my-appmap-api-key\" />\n" +
                "  <option name=\"cliEnvironment\">\n" +
                "    <map>\n" +
                "      <entry key=\"name1\" value=\"value1\" />\n" +
                "      <entry key=\"name2\" value=\"value2\" />\n" +
                "    </map>\n" +
                "  </option>\n" +
                "  <option name=\"installInstructionsViewed\" value=\"true\" />\n" +
                "</AppMapApplicationSettings>";
        Assert.assertEquals(expectedXML, JDOMUtil.write(serialized));
    }
}