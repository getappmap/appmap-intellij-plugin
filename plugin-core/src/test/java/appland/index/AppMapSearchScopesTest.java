package appland.index;

import appland.AppMapBaseTest;
import com.intellij.openapi.application.WriteAction;
import org.junit.Test;

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
}