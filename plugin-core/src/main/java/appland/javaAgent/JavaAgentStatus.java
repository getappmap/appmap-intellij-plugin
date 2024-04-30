package appland.javaAgent;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.util.text.SemVer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavaAgentStatus {
    private static final Logger LOG = Logger.getInstance(JavaAgentStatus.class);

    public static @Nullable Release.Asset getLatestAsset(@NotNull ProgressIndicator indicator) {
        final Release[] releases = {MavenRelease.INSTANCE, GitHubRelease.INSTANCE};
        for (Release release : releases) {
            try {
                var latestAsset = release.getLatest(indicator)
                        .stream()
                        .filter(asset -> "application/java-archive".equals(asset.getContentType()))
                        .findFirst()
                        .orElse(null);

                if (latestAsset != null) {
                    LOG.info("Got latest asset URL: " + latestAsset.getDownloadUrl());
                    return latestAsset;
                }
            } catch (IOException e) {
                LOG.info(String.format("Failed to query release, %s: %s", release, e));
            }
        }

        LOG.warn("Couldn't query any source for latest release.");
        return null;
    }

    public static @NotNull String generateStatusReport(ProgressIndicator indicator) {
        SemVer downloadedVersion = getDownloadedVersion();
        SemVer latestAssetVersion = getLatestAssetVersion(indicator);

        String downloadedVersionText = "Your version: ";
        downloadedVersionText += downloadedVersion == null ? "Unable to locate" : downloadedVersion;

        Path downloadLocation = getResolvedAgentFilePath();
        downloadedVersionText += downloadLocation == null ? "" : String.format("\n\nDownload location: %s", downloadLocation);

        String latestVersionText = String.format("Latest version: %s",
                latestAssetVersion == null ? "Failed to check for the latest version" : latestAssetVersion);

        return "### AppMap Java Agent Status\n\n" + latestVersionText + "\n\n\n" + downloadedVersionText + "\n\n";
    }

    private static @Nullable SemVer getDownloadedVersion() {
        Path resolvedAgentFilePath = getResolvedAgentFilePath();
        if (resolvedAgentFilePath == null) {
            return null;
        }

        String downloadedFileName = resolvedAgentFilePath.getFileName().toString();
        String version = getVersion(downloadedFileName);
        return SemVer.parseFromText(version);
    }

    private static @Nullable Path getResolvedAgentFilePath() {
        var downloadService = AppMapJavaAgentDownloadService.getInstance();
        var agentFilePath = downloadService.getAgentFilePath();

        if (!Files.exists(agentFilePath)) {
            return null;
        }

        // if a user manually added the jar file and if it isn't a symbolic link
        if (!Files.isSymbolicLink(agentFilePath)) {
            return agentFilePath;
        }

        try {
            // e.g. ~/.appmap/lib/java/appmap-1.7.2.jar
            Path resolvedSymbolicLink = Files.readSymbolicLink(agentFilePath);
            return downloadService.getAgentDirPath().resolve(resolvedSymbolicLink);
        } catch (IOException e) {
            LOG.warn("unable to locate file path for AppMap Java agent");
            return null;
        }
    }

    private static @Nullable SemVer getLatestAssetVersion(ProgressIndicator indicator) {
        Release.Asset latestAsset = getLatestAsset(indicator);
        if (latestAsset == null) {
            return null;
        }
        return SemVer.parseFromText(getVersion(latestAsset.getFileName()));
    }

    private static @Nullable String getVersion(String name) {
        // match three sets of digits separated by two periods (i.e. "1.23.45")
        String regex = "(\\d+\\.){2}\\d+";
        Matcher matcher = Pattern.compile(regex).matcher(name);

        if (matcher.find()) {
            return matcher.group();
        }

        return null;
    }
}
