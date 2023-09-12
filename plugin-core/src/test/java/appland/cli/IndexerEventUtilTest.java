package appland.cli;

import appland.AppMapBaseTest;
import org.junit.Test;

import static appland.cli.IndexerEventUtil.extractIndexedFilePath;
import static appland.cli.IndexerEventUtil.isIndexedEvent;

public class IndexerEventUtilTest extends AppMapBaseTest {
    @Test
    public void messageType() {
        // UNIX paths
        assertTrue(isIndexedEvent("Indexed /home/user/project/tmp/appmap/map1.appmap.json"));
        assertTrue(isIndexedEvent("Indexed \"/home/user/\\\"weird\\0path\\n!\\\"/tmp/appmap/map2.appmap.json\""));

        // Windows paths
        assertTrue(isIndexedEvent("Indexed C:\\Users\\Emmanuel Goldstein\\Projects\\Contoso\\tmp\\appmap\\test.appmap.json"));
        assertTrue(isIndexedEvent("Indexed \"C:\\\\Users\\\\user\\\\\\\"Important\\\" project\\\\tmp\\\\appmap\\\\test.appmap.json\""));
    }

    @Test
    public void validFilePaths() {
        // unescaped paths
        assertEquals("/home/user/project/tmp/appmap/map1.appmap.json",
                extractIndexedFilePath("Indexed /home/user/project/tmp/appmap/map1.appmap.json"));
        assertEquals("C:\\Users\\Emmanuel Goldstein\\Projects\\Contoso\\tmp\\appmap\\test.appmap.json",
                extractIndexedFilePath("Indexed C:\\Users\\Emmanuel Goldstein\\Projects\\Contoso\\tmp\\appmap\\test.appmap.json"));

        // escaped paths
        assertEquals("/home/user/\"weird\0path\n!\"/tmp/appmap/map2.appmap.json",
                extractIndexedFilePath("Indexed \"/home/user/\\\"weird\\0path\\n!\\\"/tmp/appmap/map2.appmap.json\""));
        assertEquals("C:\\Users\\user\\\"Important\" project\\tmp\\appmap\\test.appmap.json",
                extractIndexedFilePath("Indexed \"C:\\\\Users\\\\user\\\\\\\"Important\\\" project\\\\tmp\\\\appmap\\\\test.appmap.json\"\n"));
    }

    @Test
    public void invalidFilePaths() {
        assertNull(extractIndexedFilePath("\""));
        assertNull(extractIndexedFilePath("\"\""));
        assertNull(extractIndexedFilePath("Not an indexed message"));
    }
}