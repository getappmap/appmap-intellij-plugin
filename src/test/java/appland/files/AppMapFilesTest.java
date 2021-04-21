package appland.files;

import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixture4TestCase;
import org.junit.Test;

public class AppMapFilesTest extends LightPlatformCodeInsightFixture4TestCase {
    @Test
    public void files() {
        var file = myFixture.configureByText("a.json", "");
        assertFalse(AppMapFiles.isAppMap(file.getVirtualFile()));

        file = myFixture.configureByText("a.appmap.json", "");
        assertTrue(AppMapFiles.isAppMap(file.getVirtualFile()));
    }
}