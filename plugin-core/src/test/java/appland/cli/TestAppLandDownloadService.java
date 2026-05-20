package appland.cli;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.util.io.NioFiles;

import java.io.IOException;
import java.nio.file.Files;

/**
 * Customized download service for tests.
 */
public class TestAppLandDownloadService extends DefaultAppLandDownloadService {
    public static void ensureDownloaded() {
        var service = (TestAppLandDownloadService) AppLandDownloadService.getInstance();
        for (var type : CliTool.values()) {
            if (LocalAssetRepository.getInstalledVersion(type) == null) {
                service.download(type, new EmptyProgressIndicator());
            }
        }
    }

    public static void removeDownloads() {
        if (!ApplicationManager.getApplication().isUnitTestMode()) {
            throw new IllegalStateException("This method can only be called in unit test mode");
        }

        for (var type : CliTool.values()) {
            var downloadDir = LocalAssetRepository.getToolDownloadDirectory(type, true);
            if (Files.exists(downloadDir)) {
                try {
                    NioFiles.deleteRecursively(downloadDir);
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }
}
