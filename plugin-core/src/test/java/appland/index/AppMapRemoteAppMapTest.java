package appland.index;

import appland.AppMapBaseTest;
import org.junit.Test;

public class AppMapRemoteAppMapTest extends AppMapBaseTest {
    @Test
    public void index() {
        myFixture.copyFileToProject("appmap-files/Create_Owner.appmap.json");
        myFixture.copyDirectoryToProject("appmap-files/Create_Owner", "appmap-files");
        assertEquals(1, AppMapMetadataService.getInstance(getProject()).findAppMaps().size());
        assertEquals(1, AppMapMetadataService.getInstance(getProject()).findAppMaps("Create Owner").size());
    }
}