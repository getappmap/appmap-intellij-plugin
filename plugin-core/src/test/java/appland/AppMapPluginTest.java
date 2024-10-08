package appland;

import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixture4TestCase;
import org.junit.Test;

import java.nio.file.Files;

public class AppMapPluginTest extends LightPlatformCodeInsightFixture4TestCase {
    @Test
    public void pluginPath() {
        assertNotNull(AppMapPlugin.getPluginPath());
        assertTrue(Files.exists(AppMapPlugin.getPluginPath()));
    }

    @Test
    public void appMapAgentPath() {
        assertNotNull(AppMapPlugin.getAppMapJavaAgentPath());
        assertTrue(Files.isRegularFile(AppMapPlugin.getAppMapJavaAgentPath()));
    }
}