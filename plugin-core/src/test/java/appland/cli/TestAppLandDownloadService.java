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

    /**
     * Places a zero-byte executable stub for each CLI tool so tests that only need a binary to
     * exist at the installed path can run without hitting the network.
     */
    public static void ensureStubInstalled() {
        var platform = CliPlatform.currentPlatform();
        var arch = CliPlatform.currentArch();
        for (var type : CliTool.values()) {
            if (LocalAssetRepository.getInstalledBinaryPath(type) != null) {
                continue;
            }
            try {
                var cacheDir = LocalAssetRepository.getCacheDirectory(true);
                Files.createDirectories(cacheDir);
                // "stub" starts with a non-digit, so versionFromCachedFilename returns null and
                // findHighestCachedBinary ignores this file entirely.
                var stubPath = LocalAssetRepository.getExecutableFilePath(type, "stub", platform, arch, true);
                if (!Files.exists(stubPath)) {
                    Files.write(stubPath, new byte[0]);
                    CliTools.fixBinaryPermissions(stubPath);
                }
                LocalAssetRepository.updateSymlink(stubPath, LocalAssetRepository.getSymlinkPath(type));
            } catch (IOException e) {
                throw new RuntimeException("Failed to install stub binary for " + type, e);
            }
        }
    }

    /**
     * Removes the current-version symlinks so each test starts with a clean installed state.
     * The persistent binary cache is intentionally left intact so downloaded binaries survive
     * across test invocations and are not re-fetched unnecessarily.
     */
    public static void removeDownloads() {
        if (!ApplicationManager.getApplication().isUnitTestMode()) {
            throw new IllegalStateException("This method can only be called in unit test mode");
        }

        var binDir = LocalAssetRepository.getBinDirectory();
        if (Files.exists(binDir)) {
            try {
                NioFiles.deleteRecursively(binDir);
            } catch (IOException e) {
                // ignore
            }
        }
    }
}
