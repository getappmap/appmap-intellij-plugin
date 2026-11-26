package appland.webviews.navie;

import appland.AppMapBaseTest;
import com.intellij.testFramework.EdtTestUtil;
import org.junit.Test;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicReference;

public class NavieEditorProviderTest extends AppMapBaseTest {
    @Override
    protected boolean runInDispatchThread() {
        // Navie is opened from plain EDT events, which do not hold a read lock on 2026.3.
        return false;
    }

    @Test
    public void codeSelection() throws Exception {
        EdtTestUtil.runInEdtAndWait(() -> myFixture.configureByText("test.txt", "line 1\n<selection>line 2\nline 3</selection>\nline 4\n"));

        var codeSelection = buildCodeSelectionOnEdt();
        assertNotNull(codeSelection);
        assertEquals("line 2\nline 3", codeSelection.code);
        assertEquals("test.txt", codeSelection.path);
        assertEquals(2, codeSelection.lineStart);
        assertEquals(3, codeSelection.lineEnd);
        assertEquals("TEXT", codeSelection.language);
    }

    @Test
    public void noCodeSelection() throws Exception {
        EdtTestUtil.runInEdtAndWait(() -> myFixture.configureByText("test.txt", "line 1\nline 2\n"));
        assertNull(buildCodeSelectionOnEdt());
    }

    private NavieCodeSelection buildCodeSelectionOnEdt() throws InterruptedException, InvocationTargetException {
        var result = new AtomicReference<NavieCodeSelection>();
        SwingUtilities.invokeAndWait(() -> result.set(NavieEditorProvider.buildCodeSelection(getProject(), myFixture.getEditor())));
        return result.get();
    }
}
