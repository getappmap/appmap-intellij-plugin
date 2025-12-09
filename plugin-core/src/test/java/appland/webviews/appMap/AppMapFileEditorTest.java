package appland.webviews.appMap;

import appland.AppMapBaseTest;
import com.intellij.openapi.util.Disposer;
import org.junit.Assume;
import org.junit.Test;

public class AppMapFileEditorTest extends AppMapBaseTest {
    @Test
    public void disposal() {
        // JCEF is incompatible with headless environments, e.g. CI like GitHub Action
        Assume.assumeTrue(System.getenv("CI") == null);

        var file = myFixture.configureByText("appmap.json", "{}");
        var editor = new AppMapFileEditor(getProject(), file.getVirtualFile());
        assertTrue(editor.isValid());

        Disposer.dispose(editor);
        assertFalse("isValid must evaluate state of disposal", editor.isValid());
    }
}