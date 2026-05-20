package appland.cli;

import appland.AppMapBaseTest;
import org.junit.Test;

public class CliToolTest extends AppMapBaseTest {
    @Test
    public void binaryNames() {
        assertEquals("appmap-linux-x64", CliTool.AppMap.getBinaryName("linux", "x64"));
        assertEquals("appmap-macos-x64", CliTool.AppMap.getBinaryName("macos", "x64"));
        assertEquals("appmap-win-x64.exe", CliTool.AppMap.getBinaryName("win", "x64"));

        assertEquals("scanner-linux-x64", CliTool.Scanner.getBinaryName("linux", "x64"));
        assertEquals("scanner-macos-x64", CliTool.Scanner.getBinaryName("macos", "x64"));
        assertEquals("scanner-win-x64.exe", CliTool.Scanner.getBinaryName("win", "x64"));
    }
}