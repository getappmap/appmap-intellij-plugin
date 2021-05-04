package appland.index;

import appland.AppMapBaseTest;
import org.junit.Test;

import java.util.Comparator;

public class AppMapMetadataIndexTest extends AppMapBaseTest {
    @Test
    public void index() {
        myFixture.configureByText("a.appmap.json", createAppMapMetadataJSON("a"));
        myFixture.configureByText("b.appmap.json", createAppMapMetadataJSON("b"));
        myFixture.configureByText("c.appmap.json", createAppMapMetadataJSON("c"));

        var appMaps = AppMapMetadataIndex.findAppMaps(getProject());
        assertEquals(3, appMaps.size());
        appMaps.sort(Comparator.comparing(AppMapMetadata::getName));

        assertEquals(new AppMapMetadata("a", "/src/a.appmap.json"), appMaps.get(0));
        assertEquals(new AppMapMetadata("b", "/src/b.appmap.json"), appMaps.get(1));
        assertEquals(new AppMapMetadata("c", "/src/c.appmap.json"), appMaps.get(2));

        myFixture.configureByText("d.appmap.json", createAppMapMetadataJSON("d"));
        assertEquals(4, AppMapMetadataIndex.findAppMaps(getProject()).size());
    }
}