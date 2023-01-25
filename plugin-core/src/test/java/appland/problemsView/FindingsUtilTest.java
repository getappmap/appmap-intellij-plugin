package appland.problemsView;

import appland.AppMapBaseTest;
import org.junit.Test;

public class FindingsUtilTest extends AppMapBaseTest {
    @Test
    public void truncatePath() {
        assertEquals("/a/b/c", FindingsUtil.truncatePath("/a/b/c", '/'));
        assertEquals(".../long-long-long-long/long-long-long-long",
                FindingsUtil.truncatePath("/long-long-long-long/long-long-long-long/long-long-long-long/long-long-long-long", '/'));
    }
}