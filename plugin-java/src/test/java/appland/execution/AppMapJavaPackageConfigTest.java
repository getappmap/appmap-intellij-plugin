package appland.execution;

import appland.AppMapBaseTest;
import appland.config.AppMapConfigFile;
import appland.files.AppMapFiles;
import appland.index.AppMapSearchScopes;
import appland.utils.ModuleTestUtils;
import com.intellij.execution.RunManager;
import com.intellij.execution.jar.JarApplicationConfiguration;
import com.intellij.execution.jar.JarApplicationConfigurationType;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.search.GlobalSearchScopes;
import com.intellij.testFramework.PsiTestUtil;
import com.intellij.testFramework.fixtures.TempDirTestFixture;
import com.intellij.testFramework.fixtures.impl.TempDirTestFixtureImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.java.JavaResourceRootType;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class AppMapJavaPackageConfigTest extends AppMapBaseTest {
    @Test
    public void systemIndependentAppMapDir() {
        var config = AppMapJavaPackageConfig.generateAppMapConfig(getProject(), createTempDir("appmap-root"), "tmp\\appmap");
        assertEquals("tmp/appmap", config.getAppMapDir());
    }

    @Test
    public void updateConfigMustNotOverrideWithRelativePath() throws Exception {
        var contextFile = myFixture.configureByText("a.java", "").getVirtualFile();

        // wrap with withContentRoot, because the scope to locate an existing appmap.yml is based on content roots
        ModuleTestUtils.withContentRoot(getModule(), contextFile.getParent(), () -> assertConfigUpdate(contextFile, Path.of("tmp/appmap"), "tmp/appmap"));
    }

    @Test
    public void updateConfigMustNotOverrideWithAbsolutePath() throws Exception {
        var contextFile = myFixture.configureByText("a.java", "").getVirtualFile();

        // wrap with withContentRoot, because the scope to locate an existing appmap.yml is based on content roots
        var absoluteConfigPath = contextFile.toNioPath().resolveSibling("tmp/appmap").toAbsolutePath();
        ModuleTestUtils.withContentRoot(getModule(), contextFile.getParent(), () -> assertConfigUpdate(contextFile, absoluteConfigPath, "tmp/appmap"));
    }

    @Test
    public void ignoreAppMapConfigOutsideSearchScope() throws Exception {
        var contextFile = myFixture.configureByText("a.java", "").getVirtualFile();
        var contextDir = contextFile.getParent();

        // config file out of scope, must not be found
        WriteAction.runAndWait(() -> contextDir.getParent().createChildData(this, AppMapFiles.APPMAP_YML));

        ModuleTestUtils.withContentRoot(getModule(), contextFile.getParent(), () -> {
            var scope = GlobalSearchScopes.directoryScope(getProject(), contextDir, true);
            var locatedConfigFile = AppMapJavaPackageConfig.findAndUpdateAppMapConfig(contextFile, scope);
            assertNull("Update must not write to a file outside the project", locatedConfigFile);
        });
    }

    @Test
    public void ignoreResources() throws Exception {
        var rootDir = createMultiMod();

        var expectedConfig = new AppMapConfigFile();
        expectedConfig.setName("ignoreResources");
        expectedConfig.setPackages(List.of("com.example.application", "com.example.controllers", "com.example.models"));
        expectedConfig.setAppMapDir("tmp/appmap");

        ModuleTestUtils.withContentRoot(getModule(), rootDir, () -> {
            var configFilePath = AppMapJavaPackageConfig.createAppMapConfig(getModule(), rootDir, Path.of("tmp/appmap"));
            var config = AppMapConfigFile.parseConfigFile(configFilePath);
            assertEquals(expectedConfig, config);
        });
    }

    @Test
    public void configCreatedInRoot() throws Exception {
        var rootDir = createMultiMod();

        var mod = getModule();
        ModuleTestUtils.withContentRoot(mod, rootDir, () -> {
            var configPath = AppMapJavaConfigUtil.findBestAppMapContentRootDirectory(mod, rootDir.findChild("mod1"));
            var found = rootDir.findChild("appmap.yml");
            assertEquals(rootDir, configPath);
        });
    }
    private void assertConfigUpdate(@NotNull VirtualFile contextFile,
                                    @NotNull Path appMapOutputDirPath,
                                    @NotNull String expectedAppMapDir) throws IOException {
        var runConfigSettings = RunManager.getInstance(getProject()).createConfiguration("temp run config", JarApplicationConfigurationType.class);
        var runConfig = (JarApplicationConfiguration) runConfigSettings.getConfiguration();
        // update module to enforce a search scope of the run configuration
        runConfig.setModule(getModule());

        var configFilePath = AppMapJavaPackageConfig.createAppMapConfig(getModule(), contextFile.getParent(), appMapOutputDirPath);

        var config = AppMapConfigFile.parseConfigFile(configFilePath);
        assertNotNull(config);
        assertEquals(expectedAppMapDir, config.getAppMapDir());

        // update packages and write to file to verify update in the next step
        config.setPackages(List.of("appmap.a", "appmap.b"));
        config.writeTo(configFilePath);

        var updatedConfigFilePath = AppMapJavaPackageConfig.findAndUpdateAppMapConfig(contextFile, AppMapSearchScopes.appMapConfigSearchScope(getProject()));
        assertEquals("Update must write to the same file again", configFilePath, updatedConfigFilePath);

        var updatedConfig = AppMapConfigFile.parseConfigFile(configFilePath);
        assertNotNull(updatedConfig);
        assertEquals(expectedAppMapDir, updatedConfig.getAppMapDir());
        assertEquals(config.getName(), updatedConfig.getName());
        assertEquals(config.getPackages(), updatedConfig.getPackages());
    }

    @NotNull
    private VirtualFile createMultiMod() {
        var rootDir = myFixture.copyDirectoryToProject("projects/with_resources", "project");
        var myModule = getModule();
        PsiTestUtil.removeContentEntry(myModule, VirtualFileManager.getInstance().findFileByUrl("temp:///src"));
        PsiTestUtil.addSourceRoot(myModule, rootDir.findFileByRelativePath("mod1/src/main/java"));
        PsiTestUtil.addSourceRoot(myModule, rootDir.findFileByRelativePath("mod2/src/main/java"));
        PsiTestUtil.addSourceRoot(myModule, rootDir.findFileByRelativePath("mod1/src/main/resources"), JavaResourceRootType.RESOURCE);
        return rootDir;
    }
}