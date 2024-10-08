package appland.index;

import appland.AppMapBaseTest;
import com.intellij.openapi.application.WriteAction;
import org.junit.Test;

import java.io.IOException;

public class AppMapSearchScopesTest extends AppMapBaseTest {
    @Test
    public void contentRoots() throws Exception {
        var scope = AppMapSearchScopes.projectFilesWithExcluded(getProject());

        var fileA = myFixture.configureByText("a.txt", "").getVirtualFile();
        assertTrue("scope must contain source file", scope.contains(fileA));
        assertTrue("scope must contain src folder", scope.contains(fileA.getParent()));

        var topLevelDir = myFixture.getTempDirFixture().findOrCreateDir("../appmap-top-level");
        try {
            assertFalse("scope must not contain folders, which are outside content roots", scope.contains(topLevelDir));

            withContentRoot(topLevelDir, () -> {
                assertTrue("scope must contain content roots", scope.contains(topLevelDir));

                withExcludedFolder(topLevelDir, () -> {
                    assertTrue("scope must contain excluded content roots", scope.contains(topLevelDir));
                });
            });
        } finally {
            WriteAction.run(() -> topLevelDir.delete(this));
        }
    }

    @Test
    public void appMapConfigScope() throws Exception {
        var scope = AppMapSearchScopes.appMapConfigSearchScope(getProject());

        var topLevelConfig = myFixture.configureByText("appmap.yml", "content").getVirtualFile();
        assertTrue(scope.contains(topLevelConfig));
        assertTrue(scope.contains(topLevelConfig.getParent()));

        var subLevelConfig = myFixture.addFileToProject("sub-dir/appmap.yml", "content").getVirtualFile();
        assertTrue(scope.contains(subLevelConfig));

        // appmap.yml must not be found in non-content folders, e.g. in excluded folders
        var excludedDir = myFixture.getTempDirFixture().findOrCreateDir("appmap-excluded-dir");
        var excludedConfig = WriteAction.computeAndWait(() -> excludedDir.createChildData(this, "appmap.yml"));
        withExcludedFolder(excludedDir, () -> {
            assertFalse(scope.contains(excludedDir));
            assertFalse(scope.contains(excludedConfig));
        });

        // folders outside content roots must not be included
        var topLevelDir = myFixture.getTempDirFixture().findOrCreateDir("../appmap-top-level");
        assertFalse(scope.contains(topLevelDir));
    }
}