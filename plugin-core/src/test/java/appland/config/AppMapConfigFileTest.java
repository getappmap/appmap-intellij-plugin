package appland.config;

import appland.AppMapBaseTest;
import com.intellij.testFramework.fixtures.TempDirTestFixture;
import com.intellij.testFramework.fixtures.impl.TempDirTestFixtureImpl;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

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
        assertEquals("tmp/appmap", config.getAppMapDir());

        var vfsConfig = AppMapConfigFile.parseConfigFile(appMapFile);
        assertEquals(config, vfsConfig);
    }

    @Test
    public void readConfigWithoutPath() {
        var appMapFile = myFixture.copyFileToProject("appmap-config/appmap-no-dir.yml");

        var fromVirtualFile = AppMapConfigFile.parseConfigFile(appMapFile);
        assertNotNull(fromVirtualFile);
        assertNull(fromVirtualFile.getAppMapDir());

        var fromNioFile = AppMapConfigFile.parseConfigFile(appMapFile.toNioPath());
        assertNotNull(fromNioFile);
        assertNull(fromNioFile.getAppMapDir());
    }

    @Test
    public void readEmptyConfig() {
        var appMapFile = myFixture.copyFileToProject("appmap-config/appmap-empty.yml");
        assertNull(AppMapConfigFile.parseConfigFile(appMapFile));
        assertNull(AppMapConfigFile.parseConfigFile(appMapFile.toNioPath()));
    }

    @Test
    public void updateAppMapDirProperty() throws IOException {
        var appMapFile = myFixture.copyFileToProject("appmap-config/appmap-no-dir.yml");
        var config = AppMapConfigFile.parseConfigFile(appMapFile);
        assertNotNull(config);
        assertNull(config.getAppMapDir());

        // set to text value
        config.setAppMapDir("/updated/appmap/dir");
        config.writeTo(appMapFile.toNioPath());

        var updatedConfig = AppMapConfigFile.parseConfigFile(appMapFile.toNioPath());
        assertNotNull(updatedConfig);
        assertEquals("/updated/appmap/dir", updatedConfig.getAppMapDir());

        // reset to null
        config.setAppMapDir(null);
        config.writeTo(appMapFile.toNioPath());

        updatedConfig = AppMapConfigFile.parseConfigFile(appMapFile.toNioPath());
        assertNotNull(updatedConfig);
        assertNull(updatedConfig.getAppMapDir());
    }

    @Test
    public void updatePackages() throws IOException {
        var appMapFile = myFixture.copyFileToProject("appmap-config/appmap-packages.yml");
        var config = AppMapConfigFile.parseConfigFile(appMapFile);
        assertNotNull(config);

        var packages = List.of(new AppMapConfigFile.Package("package/one"), new AppMapConfigFile.Package("package/two"));
        assertEquals(packages, config.getPackages());

        var updatedPackages = List.of(new AppMapConfigFile.Package("new_package/one"), new AppMapConfigFile.Package("new_package/two"));
        config.setPackages(List.of("new_package/one", "new_package/two"));
        assertEquals(updatedPackages, config.getPackages());

        config.writeTo(appMapFile.toNioPath());
        var updatedConfig = AppMapConfigFile.parseConfigFile(appMapFile.toNioPath());
        assertNotNull(updatedConfig);
        assertEquals(updatedPackages, config.getPackages());

        // reset packages to empty list
        config.setPackages(Collections.emptyList());
        config.writeTo(appMapFile.toNioPath());
        updatedConfig = AppMapConfigFile.parseConfigFile(appMapFile.toNioPath());
        assertNotNull(updatedConfig);
        assertEquals(Collections.emptyList(), updatedConfig.getPackages());

        // reset packages to null
        config.setPackages(null);
        config.writeTo(appMapFile.toNioPath());
        updatedConfig = AppMapConfigFile.parseConfigFile(appMapFile.toNioPath());
        assertNotNull(updatedConfig);
        assertEquals(Collections.emptyList(), updatedConfig.getPackages());
    }

    @Test
    public void nameProperty() {
        var config = new AppMapConfigFile();
        assertNull(config.getName());

        config.setName("my AppMap name");
        assertEquals("my AppMap name", config.getName());
    }

    @Test
    public void languageProperty() {
        var config = new AppMapConfigFile();
        assertEquals(config.getLanguage(), "java");
    }
}