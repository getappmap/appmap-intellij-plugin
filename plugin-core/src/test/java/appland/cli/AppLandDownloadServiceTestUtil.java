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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Helper class to share code between tests.
 */
final class AppLandDownloadServiceTestUtil {
    private AppLandDownloadServiceTestUtil() {
    }

    /**
     * Downloads the given CLI tool and asserts a successful download.
     */
    static void downloadLatestCliVersions(@NotNull Project project,
                                          @NotNull CliTool toolType,
                                          @NotNull Disposable parentDisposable) throws Exception {
        var service = AppLandDownloadService.getInstance();

        var latestVersion = service.fetchLatestRemoteVersion(toolType);
        Assert.assertNotNull(latestVersion);

        var downloadSuccessful = new AtomicBoolean();
        var downloadException = new AtomicReference<Exception>();

        var latch = new CountDownLatch(1);
        ApplicationManager.getApplication()
                .getMessageBus()
                .connect(parentDisposable)
                .subscribe(AppLandDownloadListener.TOPIC, (cliTool, success) -> {
                    downloadSuccessful.set(success);
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
        Assert.assertTrue("The download must be successful", downloadSuccessful.get());
        Assert.assertNull("The download must not cause an exception", downloadException.get());
    }
}
