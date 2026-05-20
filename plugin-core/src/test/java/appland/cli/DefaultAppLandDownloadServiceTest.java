package appland.cli;

import appland.AppMapBaseTest;
import appland.AppMapDeploymentTestUtils;
import appland.deployment.AppMapDeploymentSettings;
import com.intellij.testFramework.VfsTestUtil;
import com.intellij.testFramework.fixtures.TempDirTestFixture;
import com.intellij.testFramework.fixtures.impl.TempDirTestFixtureImpl;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.List;

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
        AppMapDeploymentTestUtils.withSiteConfigFile(new AppMapDeploymentSettings(null, false, null, null), () -> {
            var status = AppLandDownloadServiceTestUtil.downloadLatestCliVersions(getProject(), CliTool.AppMap, getTestRootDisposable());
            assertEquals(AppMapDownloadStatus.Skipped, status);
        });
    }

    @Test
    public void findVersionedDownloadDirectories() {
        var downloadRoot = createTempDir("downloadRoot");
        var rootPath = downloadRoot.toNioPath();
        assertEmpty(LocalAssetRepository.findVersionDownloadDirectories(rootPath));

        // files matching the version pattern must be excluded
        VfsTestUtil.createFile(downloadRoot, "0.0.0");
        assertEmpty(LocalAssetRepository.findVersionDownloadDirectories(rootPath));

        VfsTestUtil.createDir(downloadRoot, "1.2.3");
        assertEquals(List.of(rootPath.resolve("1.2.3")), LocalAssetRepository.findVersionDownloadDirectories(rootPath));

        VfsTestUtil.createDir(downloadRoot, "something-else");
        assertEquals(List.of(rootPath.resolve("1.2.3")), LocalAssetRepository.findVersionDownloadDirectories(rootPath));

        assertEmpty(LocalAssetRepository.removeOtherVersions(rootPath, "1.2.3"));
        assertEquals(List.of(rootPath.resolve("1.2.3")), LocalAssetRepository.removeOtherVersions(rootPath, "9.9.9"));
    }

    private static void assertDownloadPath(@NotNull CliTool type) {
        assertNotNull(LocalAssetRepository.getToolDownloadDirectory(type, true));
        assertNotNull(LocalAssetRepository.getToolDownloadDirectory(type, false));
    }
}
