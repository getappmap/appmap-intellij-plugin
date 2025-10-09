package appland.cli;

import appland.AppMapBaseTest;
import com.intellij.util.text.SemVer;
import org.junit.Test;

public class AppMapUtilTest extends AppMapBaseTest {
    @Test
    public void semVerFromPath() {
        assertEquals(new SemVer("1.2.3", 1, 2, 3), CliTools.extractVersion("appmap-linux-x64-1.2.3.exe"));
        assertEquals(new SemVer("1.2.3", 1, 2, 3), CliTools.extractVersion("/home/user/.appmap/appmap-linux-x64-1.2.3"));
        assertEquals(new SemVer("1.2.3", 1, 2, 3), CliTools.extractVersion("C:\\Users\\appmap\\binaries\\appmap-linux-x64-1.2.3.exe"));
        assertNull(CliTools.extractVersion("appmap-linux-x64.exe"));
    }
}