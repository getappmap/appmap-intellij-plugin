package appland.config;

import appland.AppMapBaseTest;
import com.intellij.testFramework.fixtures.TempDirTestFixture;
import com.intellij.testFramework.fixtures.impl.TempDirTestFixtureImpl;
import org.junit.Test;

public class AppMapConfigFileTest extends AppMapBaseTest {
    @Override
    protected TempDirTestFixture createTempDirTestFixture() {
        return new TempDirTestFixtureImpl();
    }

    @Test
    public void readConfigWithPath() {
        var appMapFile = myFixture.copyFileToProject("appmap-config/appmap.yml");
        var config = AppMapConfigFile.parseConfigFile(appMapFile.toNioPath());
        assertNotNull(config);
        assertEquals("tmp/appmap", config.appMapDir);

        var vfsConfig = AppMapConfigFile.parseConfigFile(appMapFile);
        assertEquals(config, vfsConfig);
    }

    @Test
    public void readConfigWithoutPath() {
        var appMapFile = myFixture.copyFileToProject("appmap-config/appmap-no-dir.yml");
        assertNull(AppMapConfigFile.parseConfigFile(appMapFile));
        assertNull(AppMapConfigFile.parseConfigFile(appMapFile.toNioPath()));
    }

    @Test
    public void readEmptyConfig() {
        var appMapFile = myFixture.copyFileToProject("appmap-config/appmap-empty.yml");
        assertNull(AppMapConfigFile.parseConfigFile(appMapFile));
        assertNull(AppMapConfigFile.parseConfigFile(appMapFile.toNioPath()));
    }
}