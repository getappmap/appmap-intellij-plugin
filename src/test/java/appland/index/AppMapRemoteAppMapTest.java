package appland.index;

import appland.AppMapBaseTest;
import org.junit.Test;

public class AppMapRemoteAppMapTest extends AppMapBaseTest {
    @Test
    public void index() {
        myFixture.copyFileToProject("appmap-files/Create_Owner.appmap.json");
        assertEquals(1, AppMapMetadataIndex.findAppMaps(getProject(), null).size());
        assertEquals(1, AppMapMetadataIndex.findAppMaps(getProject(), "Create Owner").size());
    }
}