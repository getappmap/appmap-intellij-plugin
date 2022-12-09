package appland.webviews.findings;

import appland.AppMapBaseTest;
import appland.webviews.findingDetails.FindingDetailsEditor;
import org.junit.Test;

public class FindingDetailsEditorTest extends AppMapBaseTest {
    @Test
    public void truncatePath() {
        assertEquals("/a/b/c", FindingDetailsEditor.truncatePath("/a/b/c", '/'));
        assertEquals(".../long-long-long-long/long-long-long-long",
                FindingDetailsEditor.truncatePath("/long-long-long-long/long-long-long-long/long-long-long-long/long-long-long-long", '/'));
    }
}