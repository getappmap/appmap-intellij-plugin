package appland.cli;

import appland.AppMapBaseTest;
import org.junit.Test;

public class CliToolTest extends AppMapBaseTest {
    @Test
    public void urlsAppMap() {
        assertEquals("https://github.com/getappmap/appmap-js/releases/download/%40appland%2Fappmap-v3.32.2/appmap-linux-x64",
                CliTool.AppMap.getDownloadUrl("3.32.2", "linux", "x64"));

        assertEquals("https://github.com/getappmap/appmap-js/releases/download/%40appland%2Fappmap-v3.32.2/appmap-macos-arm64",
                CliTool.AppMap.getDownloadUrl("3.32.2", "macos", "arm64"));
    }

    @Test
    public void urlsScanner() {
        assertEquals("https://github.com/getappmap/appmap-js/releases/download/%40appland%2Fscanner-v1.67.0/scanner-linux-x64",
                CliTool.Scanner.getDownloadUrl("1.67.0", "linux", "x64"));

        assertEquals("https://github.com/getappmap/appmap-js/releases/download/%40appland%2Fscanner-v1.67.0/scanner-macos-arm64",
                CliTool.Scanner.getDownloadUrl("1.67.0", "macos", "arm64"));
    }
}