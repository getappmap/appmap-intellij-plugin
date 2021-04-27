package appland;

import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixture4TestCase;
import org.junit.Test;

public class AppMapPluginTest extends LightPlatformCodeInsightFixture4TestCase {
    @Test
    public void pluginPath() {
        assertNotNull(AppMapPlugin.getPluginPath());
        assertNotNull(AppMapPlugin.getAppMapHTMLPath());
    }
}