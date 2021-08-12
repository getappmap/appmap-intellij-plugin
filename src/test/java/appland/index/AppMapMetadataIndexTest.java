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

        var appMaps = AppMapMetadataIndex.findAppMaps(getProject(), null);
        assertEquals(3, appMaps.size());
        appMaps.sort(Comparator.comparing(AppMapMetadata::getName));

        assertEquals(new AppMapMetadata("a", "/src/a.appmap.json"), appMaps.get(0));
        assertEquals(new AppMapMetadata("b", "/src/b.appmap.json"), appMaps.get(1));
        assertEquals(new AppMapMetadata("c", "/src/c.appmap.json"), appMaps.get(2));

        myFixture.configureByText("d.appmap.json", createAppMapMetadataJSON("d"));
        assertEquals(4, AppMapMetadataIndex.findAppMaps(getProject(), null).size());

        // name filter
        assertEquals(1, AppMapMetadataIndex.findAppMaps(getProject(), "a").size());
        assertEquals(1, AppMapMetadataIndex.findAppMaps(getProject(), "b").size());
        assertEquals(1, AppMapMetadataIndex.findAppMaps(getProject(), "c").size());
        assertEquals(0, AppMapMetadataIndex.findAppMaps(getProject(), "not-present").size());

        // case-insensitive matching
        assertEquals(1, AppMapMetadataIndex.findAppMaps(getProject(), "C").size());
    }

    @Test
    public void indexWithCounts() {
        myFixture.configureByText("a.appmap.json", createAppMapMetadataJSON("a", 5, 10, 15));

        var appMaps = AppMapMetadataIndex.findAppMaps(getProject(), null);
        assertEquals(1, appMaps.size());
        assertEquals(5, appMaps.get(0).getRequestCount());
        assertEquals(10, appMaps.get(0).getQueryCount());
        assertEquals(15, appMaps.get(0).getFunctionsCount());
    }
}