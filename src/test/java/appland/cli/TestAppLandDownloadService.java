package appland.cli;

import com.intellij.openapi.progress.EmptyProgressIndicator;

/**
 * Customized download service for tests.
 */
public class TestAppLandDownloadService extends DefaultAppLandDownloadService {
    public static void ensureDownloaded() {
        var service = (TestAppLandDownloadService) AppLandDownloadService.getInstance();
        for (var value : CliTool.values()) {
            if (!service.isDownloaded(value)) {
                var version = service.fetchLatestRemoteVersion(value);
                assert version != null;

                service.download(value, version, new EmptyProgressIndicator());
            }
        }
    }
}
