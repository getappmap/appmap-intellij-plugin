package appland;

import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixture4TestCase;
import org.junit.Test;

import java.nio.file.Files;

public class AppMapPluginTest extends LightPlatformCodeInsightFixture4TestCase {
    @Test
    public void pluginPath() {
        assertNotNull(AppMapPlugin.getPluginPath());
        assertTrue(Files.exists(AppMapPlugin.getPluginPath()));

        assertNotNull(AppMapPlugin.getAppMapHTMLPath());
        assertTrue(Files.exists(AppMapPlugin.getAppMapHTMLPath()));
    }
}