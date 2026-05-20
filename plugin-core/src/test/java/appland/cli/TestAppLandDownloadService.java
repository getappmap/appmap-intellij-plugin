package appland.cli;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.util.io.NioFiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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

        Path cacheDir = LocalAssetRepository.getCacheDirectory(true);
        if (Files.exists(cacheDir)) {
            try {
                NioFiles.deleteRecursively(cacheDir);
            } catch (IOException e) {
                // ignore
            }
        }
    }
}
