package appland.installGuide;

import appland.AppMapBaseTest;
import org.junit.Ignore;
import org.junit.Test;

public class InstallGuideViewPageTest extends AppMapBaseTest {
    @Test
    public void findById() {
        assertEquals(InstallGuideViewPage.RuntimeAnalysis, InstallGuideViewPage.findByPageId("investigate-findings"));
    }

    @Ignore("ignored until GitHub AppMap supports expected exceptions")
    @Test(expected = IllegalStateException.class)
    public void findByInvalidId() {
        InstallGuideViewPage.findByPageId("invalid-id");
    }
}