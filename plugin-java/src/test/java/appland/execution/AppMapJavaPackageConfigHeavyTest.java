package appland.execution;

import appland.files.AppMapFiles;
import appland.index.AppMapSearchScopes;
import appland.utils.ModuleTestUtils;
import com.intellij.openapi.application.WriteAction;
import com.intellij.testFramework.HeavyPlatformTestCase;

import java.nio.charset.StandardCharsets;

// heavy test because light tests can't create modules
public class AppMapJavaPackageConfigHeavyTest extends HeavyPlatformTestCase {
    // Tests that appmap.yml in a top-level directory is found
    // if search starts in a different module and a directory nested inside appmap.yml's parent.
    public void testFindAppConfigInTopLevelModule() throws Exception {
        var appMapConfig = createTempVirtualFile(AppMapFiles.APPMAP_YML, null, "", StandardCharsets.UTF_8);
        ModuleTestUtils.withContentRoot(myModule, appMapConfig.getParent(), () -> {
            var nestedModule = createModule("nested-module");
            var nestedModuleContentRoot = WriteAction.compute(() -> appMapConfig.getParent().createChildDirectory(this, "nested-module-root"));

            ModuleTestUtils.withContentRoot(nestedModule, nestedModuleContentRoot, () -> {
                var projectScope = AppMapSearchScopes.appMapConfigSearchScope(getProject());
                var locatedConfigFile = AppMapJavaPackageConfig.findAndUpdateAppMapConfig(nestedModuleContentRoot, projectScope);
                assertEquals("The top-level config file must be found", appMapConfig.toNioPath(), locatedConfigFile);
            });
        });
    }
}
