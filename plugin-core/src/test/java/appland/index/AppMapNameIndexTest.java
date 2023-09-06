package appland.index;

import appland.AppMapBaseTest;
import appland.files.AppMapFiles;
import org.junit.Test;

public class AppMapNameIndexTest extends AppMapBaseTest {
    @Test
    public void nullName() throws Throwable {
        // appmap with a null value for the name JSON property in metadata.json
        var appMap = createAppMapWithIndexes("a", 0, 0, 0, null);

        var metadataDir = AppMapFiles.findAppMapMetadataDirectory(appMap);
        assertNotNull(metadataDir);

        var appMapByIndex = AppMapNameIndex.getBasicMetadata(getProject(), metadataDir);
        assertNotNull(appMapByIndex);
        assertNull(appMapByIndex.name);

        // the metadata service is skipping AppMaps without a name
        assertEquals(0, AppMapMetadataService.getInstance(getProject()).findAppMaps().size());
    }
}