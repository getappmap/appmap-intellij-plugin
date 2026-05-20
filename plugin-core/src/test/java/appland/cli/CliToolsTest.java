package appland.cli;

import appland.AppMapBaseTest;
import appland.AppMapDeploymentTestUtils;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.testFramework.fixtures.TempDirTestFixture;
import com.intellij.testFramework.fixtures.impl.TempDirTestFixtureImpl;
import org.jetbrains.annotations.NotNull;
import org.junit.Assume;
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

    @Override
    public void setUp() throws Exception {
        super.setUp();
        TestAppLandDownloadService.removeDownloads();
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
    public void installedBinaryIsPreferred() throws Exception {
        TestAppLandDownloadService.ensureDownloaded();

        var installedBinary = LocalAssetRepository.getInstalledBinaryPath(CliTool.AppMap);
        assertNotNull(installedBinary);

        // Even with a bundled binary available, the installed (symlinked) binary wins
        var bundledBinary = createMockBinary("appmap-" + CliPlatform.currentPlatform() + "-" + CliPlatform.currentArch() + "-v9999.0.0");
        AppMapDeploymentTestUtils.withBundledBinaries(List.of(bundledBinary), () -> {
            var bestMatch = CliTools.getBinaryPath(CliTool.AppMap);
            assertNotNull(bestMatch);
            assertEquals("Installed binary should be preferred over bundled",
                    installedBinary.getFileName().toString(), bestMatch.getFileName().toString());
        });
    }

    @Test
    public void symlinkCreatedAfterDownload() throws Exception {
        Assume.assumeFalse("symlinks are unreliable on Windows", SystemInfo.isWindows);

        var symlinkPath = LocalAssetRepository.getSymlinkPath(CliTool.AppMap);
        assertFalse("No symlink before download", Files.exists(symlinkPath));

        TestAppLandDownloadService.ensureDownloaded();

        assertTrue("Symlink exists after download", Files.isSymbolicLink(symlinkPath));
        assertTrue("Symlink target is an executable binary", Files.isExecutable(symlinkPath.toRealPath()));

        var version = LocalAssetRepository.getInstalledVersion(CliTool.AppMap);
        assertNotNull("Version is readable from symlink", version);
    }

    @Test
    public void secondDownloadIsSkipped() {
        var service = (TestAppLandDownloadService) AppLandDownloadService.getInstance();
        var firstStatus = service.download(CliTool.AppMap, new EmptyProgressIndicator());
        assertEquals(AppMapDownloadStatus.Successful, firstStatus);

        var secondStatus = service.download(CliTool.AppMap, new EmptyProgressIndicator());
        assertEquals("Second download should be skipped when symlink already points to the correct binary",
                AppMapDownloadStatus.Skipped, secondStatus);
    }

    @Test
    public void prereleaseVersionExtractedFromSymlink() throws Exception {
        var platform = CliPlatform.currentPlatform();
        var arch = CliPlatform.currentArch();
        var cacheDir = LocalAssetRepository.getCacheDirectory(true);
        Files.createDirectories(cacheDir);

        var binaryName = CliTool.AppMap.getBinaryName(platform, arch, "1.2.3-rc.1");
        var cachedBinary = cacheDir.resolve(binaryName);
        Files.write(cachedBinary, new byte[0]);
        CliTools.fixBinaryPermissions(cachedBinary);

        LocalAssetRepository.updateSymlink(cachedBinary, LocalAssetRepository.getSymlinkPath(CliTool.AppMap));

        assertEquals("1.2.3-rc.1", LocalAssetRepository.getInstalledVersion(CliTool.AppMap));
    }

    private @NotNull Path createMockBinary(String filename) {
        return myFixture.getTempDirFixture().createFile(filename).toNioPath();
    }
}