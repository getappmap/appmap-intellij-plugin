package appland.cli;

import com.intellij.openapi.progress.EmptyProgressIndicator;

/**
 * Customized download service for tests.
 */
public class TestAppLandDownloadService extends DefaultAppLandDownloadService {
    public static void ensureDownloaded() {
        var service = (TestAppLandDownloadService) AppLandDownloadService.getInstance();
        for (var type : CliTool.values()) {
            if (service.findLatestDownloadedVersion(type) == null) {
                var version = service.fetchLatestRemoteVersion(type);
                assert version != null;

                service.download(type, version, new EmptyProgressIndicator());
            }
        }
    }
}
