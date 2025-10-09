package appland.cli;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Helper class to share code between tests.
 */
final class AppLandDownloadServiceTestUtil {
    private AppLandDownloadServiceTestUtil() {
    }

    /**
     * Downloads the given CLI tool in a background task and asserts that the download finished.
     * If an exception was during by the download, then it will be rethrown.
     */
    static @NotNull AppMapDownloadStatus downloadLatestCliVersions(@NotNull Project project,
                                                                   @NotNull CliTool toolType,
                                                                   @NotNull Disposable parentDisposable) throws Exception {
        var service = AppLandDownloadService.getInstance();

        var latestVersion = service.fetchLatestRemoteVersion(toolType);
        Assert.assertNotNull(latestVersion);

        var downloadSuccessful = new AtomicReference<AppMapDownloadStatus>();
        var downloadException = new AtomicReference<Exception>();

        var latch = new CountDownLatch(1);
        ApplicationManager.getApplication()
                .getMessageBus()
                .connect(parentDisposable)
                .subscribe(AppLandDownloadListener.TOPIC, (cliTool, status) -> {
                    downloadSuccessful.set(status);
                    latch.countDown();
                });

        new Task.Backgroundable(project, "Downloading", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    service.download(toolType, latestVersion, indicator);
                } catch (Exception e) {
                    downloadException.set(e);
                }
            }
        }.queue();

        var ok = latch.await(5, TimeUnit.MINUTES);
        Assert.assertTrue("The download must finish", ok);

        if (downloadException.get() != null) {
            throw downloadException.get();
        }
        return downloadSuccessful.get();
    }

    /**
     * Downloads the given CLI tool and asserts a successful download.
     */
    static void assertDownloadLatestCliVersions(@NotNull Project project,
                                                @NotNull CliTool toolType,
                                                @NotNull Disposable parentDisposable) throws Exception {
        var status = downloadLatestCliVersions(project, toolType, parentDisposable);
        Assert.assertTrue("The download must be successful", status.isSuccessful());
    }
}
