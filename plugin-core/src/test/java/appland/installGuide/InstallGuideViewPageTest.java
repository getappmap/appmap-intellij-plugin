package appland.installGuide;

import appland.AppMapBaseTest;
import org.junit.Ignore;
import org.junit.Test;

public class InstallGuideViewPageTest extends AppMapBaseTest {
    @Test
    public void findById() {
        assertEquals(InstallGuideViewPage.InstallAgent, InstallGuideViewPage.findByPageId("project-picker"));
    }

    @Ignore("ignored until GitHub AppMap supports expected exceptions")
    @Test(expected = IllegalStateException.class)
    public void findByInvalidId() {
        InstallGuideViewPage.findByPageId("invalid-id");
    }
}