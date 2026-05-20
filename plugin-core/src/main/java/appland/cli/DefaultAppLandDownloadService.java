package appland.cli;

import appland.AppMapBundle;
import appland.github.GitHubHttpRequests;
import appland.settings.DownloadSettings;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.util.io.HttpRequests;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;

/**
 * Downloads are cached as "$NAME-$OS-$ARCH-$VERSION" in the platform-specific cache directory,
 * e.g. "appmap-linux-x64-1.2.3" in ~/.cache/appmap/ on Linux.
 * This matches the layout used by the VSCode AppMap extension so both share the same cache.
 */
public class DefaultAppLandDownloadService implements AppLandDownloadService {
    private static final Logger LOG = Logger.getInstance(DefaultAppLandDownloadService.class);

    @Override
    public void queueDownloadTasks(@NotNull Project project) {
        if (DownloadSettings.isAssetDownloadEnabled()) {
            downloadTool(project, CliTool.AppMap);
            downloadTool(project, CliTool.Scanner);
        }
    }

    @NotNull AppMapDownloadStatus download(@NotNull CliTool type, @NotNull ProgressIndicator progressIndicator) {
        if (DownloadSettings.isAssetDownloadDisabled()) {
            notifyDownloadFinished(type, AppMapDownloadStatus.Skipped);
            return AppMapDownloadStatus.Skipped;
        }

        var unitTestMode = ApplicationManager.getApplication().isUnitTestMode();
        var manifest = ManifestManager.fetch(type);
        var platform = CliPlatform.currentPlatform();
        var arch = CliPlatform.currentArch();

        if (manifest == null) {
            LOG.warn("Cannot fetch manifest for " + type.getId());
            // Symlink already works — nothing to do.
            if (LocalAssetRepository.getInstalledBinaryPath(type) != null) {
                notifyDownloadFinished(type, AppMapDownloadStatus.Skipped);
                return AppMapDownloadStatus.Skipped;
            }

            // Symlink is missing or broken — restore from the highest cached binary.
            var cachedPath = LocalAssetRepository.findHighestCachedBinary(type, platform, arch, unitTestMode);
            if (cachedPath != null) {
                LOG.info("Restoring " + type.getId() + " symlink to cached " + LocalAssetRepository.versionFromPath(cachedPath));
                LocalAssetRepository.updateSymlink(cachedPath, LocalAssetRepository.getSymlinkPath(type));
                notifyDownloadFinished(type, AppMapDownloadStatus.Successful);
                return AppMapDownloadStatus.Successful;
            }

            // A bundled binary (from deployment settings) is still usable even without a download.
            if (CliTools.getBinaryPath(type, platform, arch) != null) {
                notifyDownloadFinished(type, AppMapDownloadStatus.Skipped);
                return AppMapDownloadStatus.Skipped;
            }

            notifyDownloadFinished(type, AppMapDownloadStatus.Failed);
            return AppMapDownloadStatus.Failed;
        }

        var version = manifest.version;

        if (LocalAssetRepository.isDownloaded(type, version, platform, arch, unitTestMode)) {
            var cachedPath = LocalAssetRepository.getExecutableFilePath(type, version, platform, arch, unitTestMode);
            var symlinkPath = LocalAssetRepository.getSymlinkPath(type);
            if (!LocalAssetRepository.symlinkPointsTo(symlinkPath, cachedPath)) {
                LOG.info("Updating " + type.getId() + " symlink to version " + version);
                LocalAssetRepository.updateSymlink(cachedPath, symlinkPath);
                notifyDownloadFinished(type, AppMapDownloadStatus.Successful);
                return AppMapDownloadStatus.Successful;
            }
            notifyDownloadFinished(type, AppMapDownloadStatus.Skipped);
            return AppMapDownloadStatus.Skipped;
        }

        var platformId = CliPlatform.getId();
        var source = manifest.getAsset(platformId);
        if (source == null) {
            LOG.warn("No " + type.getId() + " asset for " + platformId + " in manifest");
            notifyDownloadFinished(type, AppMapDownloadStatus.Failed);
            return AppMapDownloadStatus.Failed;
        }

        var targetFilePath = LocalAssetRepository.getExecutableFilePath(type, version, platform, arch, unitTestMode);
        var downloadTargetFilePath = targetFilePath.resolveSibling(targetFilePath.getFileName().toString() + ".download");

        try {
            Files.createDirectories(targetFilePath.getParent());
            Files.deleteIfExists(targetFilePath);
            Files.deleteIfExists(downloadTargetFilePath);

            LOG.info(String.format("Downloading CLI binary %s from %s to %s", type.getId(), source.url, targetFilePath));
            HttpRequests.request(source.url)
                    .tuner(GitHubHttpRequests.gitHubTokenTuner())
                    .saveToFile(downloadTargetFilePath, progressIndicator);

            if (source.digest != null) {
                if (!verifyDigest(downloadTargetFilePath, source.digest)) {
                    LOG.warn("Digest verification failed for " + type.getId() + " version " + version);
                    Files.deleteIfExists(downloadTargetFilePath);
                    notifyDownloadFinished(type, AppMapDownloadStatus.Failed);
                    return AppMapDownloadStatus.Failed;
                }
                LOG.info("Verified digest for " + type.getId() + " version " + version);
            }

            CliTools.fixBinaryPermissions(downloadTargetFilePath);
            Files.move(downloadTargetFilePath, targetFilePath, StandardCopyOption.ATOMIC_MOVE);
            LocalAssetRepository.updateSymlink(targetFilePath, LocalAssetRepository.getSymlinkPath(type));

            notifyDownloadFinished(type, AppMapDownloadStatus.Successful);
            return AppMapDownloadStatus.Successful;
        } catch (Exception e) {
            LOG.debug("Error downloading CLI binary", e);

            try {
                Files.deleteIfExists(targetFilePath);
                Files.deleteIfExists(downloadTargetFilePath);
            } catch (IOException ex) {
                LOG.debug("Error cleaning up failed download: " + targetFilePath, ex);
            }

            notifyDownloadFinished(type, AppMapDownloadStatus.Failed);
            return AppMapDownloadStatus.Failed;
        }
    }

    private boolean verifyDigest(Path path, String expectedDigest) throws Exception {
        var parts = expectedDigest.split(":", 2);
        if (parts.length != 2 || !parts[0].equals("sha256")) {
            LOG.warn("Unsupported digest algorithm: " + (parts.length > 0 ? parts[0] : "none"));
            return false;
        }
        var expectedHash = parts[1];

        var digest = MessageDigest.getInstance("SHA-256");
        try (InputStream is = Files.newInputStream(path)) {
            var buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }
        var hashBytes = digest.digest();
        var hexString = new StringBuilder(2 * hashBytes.length);
        for (byte b : hashBytes) {
            var hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }

        return expectedHash.equalsIgnoreCase(hexString.toString());
    }

    private void downloadTool(@NotNull Project project, @NotNull CliTool type) {
        var title = AppMapBundle.get("cliDownload.progress.title", type.getPresentableName());
        new Task.Backgroundable(project, title, true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                var status = download(type, indicator);
                switch (status) {
                    case Failed -> LOG.warn("Download of CLI tool failed: " + type);
                    case Skipped -> LOG.debug("Download of CLI tool was skipped: " + type);
                }
            }
        }.queue();
    }

    private static void notifyDownloadFinished(@NotNull CliTool type, AppMapDownloadStatus status) {
        var application = ApplicationManager.getApplication();
        if (!application.isDisposed()) {
            application.executeOnPooledThread(() -> {
                if (!application.isDisposed()) {
                    application.getMessageBus()
                            .syncPublisher(AppLandDownloadListener.TOPIC)
                            .downloadFinished(type, status);
                }
            });
        }
    }
}
