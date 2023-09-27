package appland.execution;

import appland.AppMapBaseTest;
import appland.config.AppMapConfigFile;
import com.intellij.execution.RunManager;
import com.intellij.execution.jar.JarApplicationConfiguration;
import com.intellij.execution.jar.JarApplicationConfigurationType;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.fixtures.TempDirTestFixture;
import com.intellij.testFramework.fixtures.impl.TempDirTestFixtureImpl;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class AppMapJavaPackageConfigTest extends AppMapBaseTest {
    @Override
    protected TempDirTestFixture createTempDirTestFixture() {
        // create temp files on disk
        return new TempDirTestFixtureImpl();
    }

    @Test
    public void systemIndependentAppMapDir() {
        var config = AppMapJavaPackageConfig.generateAppMapConfig(getModule(), "tmp\\appmap");
        assertEquals("tmp/appmap", config.getAppMapDir());
    }

    @Test
    public void updateConfigMustNotOverrideWithRelativePath() throws Exception {
        var contextFile = myFixture.configureByText("a.java", "").getVirtualFile();

        // wrap with withContentRoot, because the scope to locate an existing appmap.yml is based on content roots
        withContentRoot(getModule(), contextFile.getParent(), () -> assertConfigUpdate(contextFile, Path.of("tmp/appmap"), "tmp/appmap"));
    }

    @Test
    public void updateConfigMustNotOverrideWithAbsolutePath() throws Exception {
        var contextFile = myFixture.configureByText("a.java", "").getVirtualFile();

        // wrap with withContentRoot, because the scope to locate an existing appmap.yml is based on content roots
        var absoluteConfigPath = contextFile.toNioPath().resolveSibling("tmp/appmap").toAbsolutePath();
        withContentRoot(getModule(), contextFile.getParent(), () -> assertConfigUpdate(contextFile, absoluteConfigPath, "tmp/appmap"));
    }

    private void assertConfigUpdate(@NotNull VirtualFile contextFile,
                                    @NotNull Path appMapOutputDirPath,
                                    @NotNull String expectedAppMapDir) throws IOException {
        var runConfigSettings = RunManager.getInstance(getProject()).createConfiguration("temp run config", JarApplicationConfigurationType.class);
        var runConfig = (JarApplicationConfiguration) runConfigSettings.getConfiguration();
        // update module to enforce a search scope of the run configuration
        runConfig.setModule(getModule());

        var configFilePath = AppMapJavaPackageConfig.createOrUpdateAppMapConfig(getModule(),
                contextFile,
                appMapOutputDirPath);

        var config = AppMapConfigFile.parseConfigFile(configFilePath);
        assertNotNull(config);
        assertEquals(expectedAppMapDir, config.getAppMapDir());

        // update packages and write to file to verify update in the next step
        config.setPackages(List.of("appmap.a", "appmap.b"));
        config.writeTo(configFilePath);

        var updatedConfigFilePath = AppMapJavaPackageConfig.createOrUpdateAppMapConfig(getModule(),
                contextFile,
                Path.of(appMapOutputDirPath + "-updated"));
        assertEquals("Update must write to the same file again", configFilePath, updatedConfigFilePath);

        var updatedConfig = AppMapConfigFile.parseConfigFile(configFilePath);
        assertNotNull(updatedConfig);
        assertEquals(expectedAppMapDir + "-updated", updatedConfig.getAppMapDir());
        assertEquals(config.getName(), updatedConfig.getName());
        assertEquals(config.getPackages(), updatedConfig.getPackages());
    }
}