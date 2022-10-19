package appland.cli;

import appland.AppMapBaseTest;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.testFramework.VfsTestUtil;
import com.intellij.testFramework.fixtures.TempDirTestFixture;
import com.intellij.testFramework.fixtures.impl.TempDirTestFixtureImpl;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
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
    public void downloadBinary() throws IOException, InterruptedException {
        var type = CliTool.AppMap;
        var service = AppLandDownloadService.getInstance();

        var latestVersion = service.fetchLatestRemoteVersion(type);
        assertNotNull(latestVersion);

        var latch = new CountDownLatch(1);
        ApplicationManager.getApplication().getMessageBus().connect(getTestRootDisposable())
                .subscribe(AppLandDownloadListener.TOPIC, (cliTool, success) -> {
                    if (!success) {
                        addSuppressedException(new IllegalStateException("CLI download failed: " + cliTool));
                    }
                    latch.countDown();
                });

        new Task.Backgroundable(getProject(), "Downloading", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    service.download(type, latestVersion, indicator);
                } catch (Exception e) {
                    addSuppressedException(e);
                }
            }
        }.queue();

        var ok = latch.await(5, TimeUnit.MINUTES);
        assertTrue("Download must succeed", ok);

        assertTrue(service.isDownloaded(type, latestVersion, ApplicationManager.getApplication().isUnitTestMode()));
        assertTrue(service.isDownloaded(type));
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