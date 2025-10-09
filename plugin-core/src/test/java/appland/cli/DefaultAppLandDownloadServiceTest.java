package appland.cli;

import appland.AppMapBaseTest;
import appland.AppMapDeploymentTestUtils;
import appland.deployment.AppMapDeploymentSettings;
import com.intellij.testFramework.VfsTestUtil;
import com.intellij.testFramework.fixtures.TempDirTestFixture;
import com.intellij.testFramework.fixtures.impl.TempDirTestFixtureImpl;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

public class DefaultAppLandDownloadServiceTest extends AppMapBaseTest {
    @Override
    protected boolean runInDispatchThread() {
        return false;
    }

    @Override
    protected TempDirTestFixture createTempDirTestFixture() {
        return new TempDirTestFixtureImpl();
    }

    @Test
    public void latestVersion() throws IOException {
        assertVersion(CliTool.AppMap);
        assertVersion(CliTool.Scanner);
    }

    @Test
    public void downloadPath() {
        assertDownloadPath(CliTool.AppMap);
        assertDownloadPath(CliTool.Scanner);
    }

    @Test
    public void downloadAppMapBinary() throws Exception {
        // We're only testing the download of the AppMap tool and not of the scanner
        // because they work in the same way, and we don't want to extend the test duration unnecessarily.
        AppLandDownloadServiceTestUtil.assertDownloadLatestCliVersions(getProject(), CliTool.AppMap, getTestRootDisposable());
    }

    @Test
    public void downloadAppMapBinaryWithProhibitingDeploymentSettings() throws Exception {
        AppMapDeploymentTestUtils.withSiteConfigFile(new AppMapDeploymentSettings(null, false), () -> {
            var status = AppLandDownloadServiceTestUtil.downloadLatestCliVersions(getProject(), CliTool.AppMap, getTestRootDisposable());
            assertEquals(AppMapDownloadStatus.Skipped, status);
        });
    }

    @Test
    public void findVersionedDownloadDirectories() {
        var downloadRoot = createTempDir("downloadRoot");
        var rootPath = downloadRoot.toNioPath();
        assertEmpty(DefaultAppLandDownloadService.findVersionDownloadDirectories(rootPath));

        // files matching the version pattern must be excluded
        VfsTestUtil.createFile(downloadRoot, "0.0.0");
        assertEmpty(DefaultAppLandDownloadService.findVersionDownloadDirectories(rootPath));

        VfsTestUtil.createDir(downloadRoot, "1.2.3");
        assertEquals(List.of(rootPath.resolve("1.2.3")), DefaultAppLandDownloadService.findVersionDownloadDirectories(rootPath));

        VfsTestUtil.createDir(downloadRoot, "something-else");
        assertEquals(List.of(rootPath.resolve("1.2.3")), DefaultAppLandDownloadService.findVersionDownloadDirectories(rootPath));

        assertEmpty(DefaultAppLandDownloadService.removeOtherVersions(rootPath, "1.2.3"));
        assertEquals(List.of(rootPath.resolve("1.2.3")), DefaultAppLandDownloadService.removeOtherVersions(rootPath, "9.9.9"));
    }

    private static void assertDownloadPath(@NotNull CliTool type) {
        assertNotNull(DefaultAppLandDownloadService.getToolDownloadDirectory(type, true));
        assertNotNull(DefaultAppLandDownloadService.getToolDownloadDirectory(type, false));
    }

    private static void assertVersion(@NotNull CliTool type) throws IOException {
        var version = AppLandDownloadService.getInstance().fetchLatestRemoteVersion(type);
        assertNotNull(version);
        assertTrue(Pattern.matches("\\d+\\.\\d+\\.\\d+", version));
    }
}