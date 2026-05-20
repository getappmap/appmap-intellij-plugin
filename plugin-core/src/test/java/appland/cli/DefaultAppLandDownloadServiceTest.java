package appland.cli;

import appland.AppMapBaseTest;
import appland.AppMapDeploymentTestUtils;
import appland.deployment.AppMapDeploymentSettings;
import com.intellij.testFramework.fixtures.TempDirTestFixture;
import com.intellij.testFramework.fixtures.impl.TempDirTestFixtureImpl;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

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

    private static void assertDownloadPath(@NotNull CliTool type) {
        assertNotNull(LocalAssetRepository.getToolDownloadDirectory(type, true));
        assertNotNull(LocalAssetRepository.getToolDownloadDirectory(type, false));
    }
}
