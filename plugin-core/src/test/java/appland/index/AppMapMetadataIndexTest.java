package appland.index;

import appland.AppMapBaseTest;
import org.junit.Test;

import java.util.Comparator;

public class AppMapMetadataIndexTest extends AppMapBaseTest {
    @Test
    public void index() throws Throwable {
        var appMapA = createAppMapWithIndexes("a");
        var appMapB = createAppMapWithIndexes("b");
        var appMapC = createAppMapWithIndexes("c");

        var appMaps = AppMapMetadataService.getInstance(getProject()).findAppMaps();
        assertEquals(3, appMaps.size());
        appMaps.sort(Comparator.comparing(AppMapMetadata::getName));

        assertEquals(new AppMapMetadata("a", appMapA), appMaps.get(0));
        assertEquals(new AppMapMetadata("b", appMapB), appMaps.get(1));
        assertEquals(new AppMapMetadata("c", appMapC), appMaps.get(2));

        createAppMapWithIndexes("d");
        assertEquals(4, AppMapMetadataService.getInstance(getProject()).findAppMaps().size());

        // name filter
        assertEquals(1, AppMapMetadataService.getInstance(getProject()).findAppMaps("a").size());
        assertEquals(1, AppMapMetadataService.getInstance(getProject()).findAppMaps("b").size());
        assertEquals(1, AppMapMetadataService.getInstance(getProject()).findAppMaps("c").size());
        assertEquals(0, AppMapMetadataService.getInstance(getProject()).findAppMaps("not-present").size());

        // case-insensitive matching
        assertEquals(1, AppMapMetadataService.getInstance(getProject()).findAppMaps("C").size());
    }

    @Test
    public void indexWithCounts() throws Throwable {
        createAppMapWithIndexes("a", 5, 10, 15, "a");

        var appMaps = AppMapMetadataService.getInstance(getProject()).findAppMaps();
        assertEquals(1, appMaps.size());
        assertEquals(5, appMaps.get(0).getRequestCount());
        assertEquals(10, appMaps.get(0).getQueryCount());
        assertEquals(15, appMaps.get(0).getFunctionsCount());
    }
}