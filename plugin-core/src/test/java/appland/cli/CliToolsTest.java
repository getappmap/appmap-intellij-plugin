package appland.cli;

import appland.AppMapBaseTest;
import appland.AppMapDeploymentTestUtils;
import appland.deployment.AppMapDeploymentSettingsService;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.testFramework.fixtures.TempDirTestFixture;
import com.intellij.testFramework.fixtures.impl.TempDirTestFixtureImpl;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class CliToolsTest extends AppMapBaseTest {
    @Override
    public TempDirTestFixture createTempDirTestFixture() {
        // create temp files on disk
        return new TempDirTestFixtureImpl();
    }

    @Override
    protected boolean runInDispatchThread() {
        return false;
    }

    @Test
    public void bestBinarySearch() throws Exception {
        // Mock binaries to copy into the location where bundled binaries are searched
        // The high version numbers ensure that a binary downloaded by our tests are not returned.
        var appmapBinaries = List.of(
                createMockBinary("appmap-win-x64-v1000.2.0.exe"),
                createMockBinary("appmap-win-x64-v1000.1.123.exe"),
                createMockBinary("appmap-win-arm64-v1000.2.0.exe"),
                createMockBinary("appmap-win-arm64-v1000.1.123.exe"),

                createMockBinary("appmap-linux-x64-v1000.2.0"),
                createMockBinary("appmap-linux-x64-v1000.1.123"),
                createMockBinary("appmap-linux-arm64-v1000.2.0"),
                createMockBinary("appmap-linux-arm64-v1000.1.123"),

                createMockBinary("appmap-macos-x64-v1000.2.0"),
                createMockBinary("appmap-macos-x64-v1000.1.123"),
                createMockBinary("appmap-macos-arm64-v1000.2.0"),
                createMockBinary("appmap-macos-arm64-v1000.1.123")
        );

        AppMapDeploymentTestUtils.withBundledBinaries(appmapBinaries, () -> {
            assertNotNull("There must be a match for every platform", CliTools.getBinaryPath(CliTool.AppMap));

            var windowsMatch = CliTools.getBinaryPath(CliTool.AppMap, "win", "x64");
            assertNotNull(windowsMatch);
            assertEquals("appmap-win-x64-v1000.2.0.exe", windowsMatch.getFileName().toString());

            var macosMatch = CliTools.getBinaryPath(CliTool.AppMap, "macos", "arm64");
            assertNotNull(macosMatch);
            assertEquals("appmap-macos-arm64-v1000.2.0", macosMatch.getFileName().toString());
            if (SystemInfo.isMac) {
                assertTrue(Files.isExecutable(macosMatch));
            }

            var linuxMatch = CliTools.getBinaryPath(CliTool.AppMap, "linux", "x64");
            assertNotNull(linuxMatch);
            assertEquals("appmap-linux-x64-v1000.2.0", linuxMatch.getFileName().toString());
            if (SystemInfo.isLinux) {
                assertTrue(Files.isExecutable(linuxMatch));
            }
        });
    }

    @Test
    public void bundledBinaryIsPreferred() throws Exception {
        TestAppLandDownloadService.ensureDownloaded();

        var downloadedAppMapBinary = AppLandDownloadService.getInstance().getDownloadFilePath(CliTool.AppMap,
                CliTools.currentPlatform(),
                CliTools.currentArch());
        assertNotNull(downloadedAppMapBinary);

        var bundledBinary = createMockBinary(downloadedAppMapBinary.getFileName().toString());

        AppMapDeploymentTestUtils.withBundledBinaries(List.of(bundledBinary), () -> {
            var bestMatch = CliTools.getBinaryPath(CliTool.AppMap);
            assertNotNull(bestMatch);
            assertEquals(bundledBinary.getFileName().toString(), bestMatch.getFileName().toString());

            var binaryParentDir = AppMapDeploymentSettingsService.bundledBinarySearchPath().stream().findFirst().orElse(null);
            assertNotNull(binaryParentDir);
            assertEquals(
                    "Bundled binaries should be preferred over downloaded binaries",
                    binaryParentDir.resolve(bestMatch.getFileName()),
                    bestMatch);
        });
    }

    private @NotNull Path createMockBinary(String filename) {
        return myFixture.getTempDirFixture().createFile(filename).toNioPath();
    }
}