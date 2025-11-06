package appland.cli;

import appland.AppMapBundle;
import appland.settings.DownloadSettings;
import appland.utils.GsonUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.Version;
import com.intellij.openapi.util.io.NioFiles;
import com.intellij.util.Urls;
import com.intellij.util.io.HttpRequests;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static appland.cli.CliTools.currentArch;
import static appland.cli.CliTools.currentPlatform;

/**
 * Downloads are stored as "appland-downloads/$NAME/$VERSION/$NAME-$OS-$ARCH", e.g. "appland-downloads/appmap/1.2.3/appmap-linux-x64".
 */
public class DefaultAppLandDownloadService implements AppLandDownloadService {
    private static final Logger LOG = Logger.getInstance(DefaultAppLandDownloadService.class);
    private static final String LATEST_VERSION_URL = "https://api.github.com/repos/getappmap/appmap-js/releases";
    private final Object lock = new Object();
    private volatile JsonArray cachedReleases = null;

    @Override
    public @Nullable Path getDownloadFilePath(@NotNull CliTool type, @NotNull String platform, @NotNull String arch) {
        var version = findLatestDownloadedVersion(type);
        if (version == null) {
            return null;
        }

        var unitTestMode = ApplicationManager.getApplication().isUnitTestMode();
        return getExecutableFilePath(type, version, platform, arch, unitTestMode);
    }

    @Override
    public @NotNull AppMapDownloadStatus download(@NotNull CliTool type,
                                                  @NotNull String version,
                                                  @NotNull ProgressIndicator progressIndicator) {
        if (DownloadSettings.isAssetDownloadDisabled()) {
            notifyDownloadFinished(type, AppMapDownloadStatus.Skipped);
            return AppMapDownloadStatus.Skipped;
        }

        var unitTestMode = ApplicationManager.getApplication().isUnitTestMode();

        try {
            var platform = currentPlatform();
            var arch = currentArch();

            // the path to the downloaded executable
            var targetFilePath = getExecutableFilePath(type, version, platform, arch, unitTestMode);
            // the path where the in-progress download is stored
            var downloadTargetFilePath = targetFilePath.resolveSibling(targetFilePath.getFileName().toString() + ".download");

            // final and temp file paths have the same parent
            Files.createDirectories(targetFilePath.getParent());
            Files.deleteIfExists(targetFilePath);
            Files.deleteIfExists(downloadTargetFilePath);

            // download the file, it throws an IOException if it fails or if the remote file is unavailable
            var url = type.getDownloadUrl(version, platform, arch);
            LOG.debug(String.format("Downloading CLI binary %s from %s to %s", type.getId(), url, targetFilePath));
            HttpRequests.request(url).saveToFile(downloadTargetFilePath, progressIndicator);

            CliTools.fixBinaryPermissions(downloadTargetFilePath);

            // now move the downloaded file to the expected path
            Files.move(downloadTargetFilePath, targetFilePath, StandardCopyOption.ATOMIC_MOVE);

            // after a successful download, remove previous downloads, make sure to keep the new download
            removeOtherVersions(type, version, unitTestMode);

            notifyDownloadFinished(type, AppMapDownloadStatus.Successful);
            return AppMapDownloadStatus.Successful;
        } catch (IOException e) {
            LOG.debug("Error downloading CLI binary", e);

            // cleanup failed download, if available
            var downloadDir = getVersionDownloadDirectory(type, version, unitTestMode);
            try {
                if (Files.isDirectory(downloadDir)) {
                    NioFiles.deleteRecursively(downloadDir);
                }
            } catch (IOException ex) {
                LOG.debug("Error cleaning up download directory: " + downloadDir, e);
            }

            notifyDownloadFinished(type, AppMapDownloadStatus.Failed);
            return AppMapDownloadStatus.Failed;
        }
    }

    @Override
    public @Nullable String fetchLatestRemoteVersion(@NotNull CliTool type) {
        var releases = getLatestReleases();
        if (releases == null) {
            return null;
        }
        return parseLatestVersion(type, releases);
    }

    public static @Nullable String parseLatestVersion(@NotNull CliTool type, @NotNull com.google.gson.JsonArray releases) {
        var prefix = "@appland/" + type.getId() + "-v";
        for (var release : releases) {
            var releaseObj = release.getAsJsonObject();
            if (releaseObj.has("tag_name")) {
                var tagName = releaseObj.getAsJsonPrimitive("tag_name").getAsString();
                if (tagName.startsWith(prefix)) {
                    return tagName.substring(prefix.length());
                }
            }
        }
        return null;
    }

    private @Nullable JsonArray getLatestReleases() {
        if (cachedReleases != null) {
            return cachedReleases;
        }

        synchronized (lock) {
            if (cachedReleases != null) {
                return cachedReleases;
            }

            try {
                var request = HttpRequests.request(Urls.newFromEncoded(LATEST_VERSION_URL));
                var jsonString = request.readString(ProgressManager.getGlobalProgressIndicator());
                var json = GsonUtils.GSON.fromJson(jsonString, com.google.gson.JsonArray.class);
                if (json != null && json.isJsonArray()) {
                    cachedReleases = json.getAsJsonArray();
                    return cachedReleases;
                }
                return null;
            } catch (IOException e) {
                return null;
            }
        }
    }

    @Override
    public void queueDownloadTasks(@NotNull Project project) {
        if (DownloadSettings.isAssetDownloadEnabled()) {
            downloadTool(project, CliTool.AppMap);
            downloadTool(project, CliTool.Scanner);
        }
    }

    public @Nullable String findLatestDownloadedVersion(@NotNull CliTool type) {
        var directory = getToolDownloadDirectory(type, ApplicationManager.getApplication().isUnitTestMode());
        if (!Files.isDirectory(directory)) {
            return null;
        }

        var candidates = findVersionDownloadDirectories(directory);
        if (candidates.isEmpty()) {
            return null;
        }

        return candidates
                .stream()
                .map(file -> file.getFileName().toString())
                .max(Comparator.naturalOrder())
                .orElse(null);
    }

    private boolean isDownloaded(@NotNull CliTool type, @NotNull String version, boolean unitTestMode) {
        return isExecutableBinary(getExecutableFilePath(type, version, currentPlatform(), currentArch(), unitTestMode));
    }

    private boolean isExecutableBinary(@NotNull Path file) {
        return Files.isRegularFile(file) && (!SystemInfo.isUnix || Files.isExecutable(file));
    }

    static @NotNull List<Path> findVersionDownloadDirectories(@NotNull Path parentDirectory) {
        try (var stream = Files.list(parentDirectory)) {
            return stream
                    .filter(file -> Files.isDirectory(file) && Version.parseVersion(file.getFileName().toString()) != null)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            LOG.debug("Error listing download directory entries: " + parentDirectory, e);
            return Collections.emptyList();
        }
    }

    static void removeOtherVersions(@NotNull CliTool type, @NotNull String keepVersion, boolean unitTestMode) {
        removeOtherVersions(getToolDownloadDirectory(type, unitTestMode), keepVersion);
    }

    @NotNull
    static List<Path> removeOtherVersions(@NotNull Path directory, @NotNull String keepVersion) {
        var toRemove = findVersionDownloadDirectories(directory)
                .stream()
                .filter(path -> !keepVersion.equals(path.getFileName().toString()))
                .collect(Collectors.toList());

        if (!toRemove.isEmpty()) {
            for (var path : toRemove) {
                try {
                    NioFiles.deleteRecursively(path);
                } catch (IOException e) {
                    LOG.debug("Error deleting download directory: " + path, e);
                }
            }
        }
        return toRemove;
    }

    static @NotNull Path getExecutableFilePath(@NotNull CliTool type,
                                               @NotNull String version,
                                               @NotNull String platform,
                                               @NotNull String arch,
                                               boolean unitTestMode) {
        return getVersionDownloadDirectory(type, version, unitTestMode).resolve(type.getBinaryName(platform, arch));
    }

    static @NotNull Path getVersionDownloadDirectory(@NotNull CliTool type, @NotNull String version, boolean unitTestMode) {
        return getToolDownloadDirectory(type, unitTestMode).resolve(version);
    }

    @NotNull
    static Path getToolDownloadDirectory(@NotNull CliTool type, boolean unitTestMode) {
        var basePath = unitTestMode || "true".equals(System.getProperty("appmap.sandbox"))
                ? Paths.get(PathManager.getTempPath()).resolve("appland-downloads")
                : Paths.get(PathManager.getDefaultSystemPathFor("appland-plugin"));
        return basePath.resolve(type.getId());
    }

    private void downloadTool(@NotNull Project project, @NotNull CliTool type) {
        LOG.debug("Downloading AppMap CLI tool: " + type);
        var latestVersion = fetchLatestRemoteVersion(type);
        if (latestVersion != null && !isDownloaded(type, latestVersion, ApplicationManager.getApplication().isUnitTestMode())) {
            var title = AppMapBundle.get("cliDownload.progress.title", type.getPresentableName());
            new Task.Backgroundable(project, title, true) {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    var status = download(type, latestVersion, indicator);
                    switch (status) {
                        case Failed -> LOG.debug("Download of CLI tool failed: " + type);
                        case Skipped -> LOG.debug("Download of CLI tool was skipped: " + type);
                    }
                }
            }.queue();
        }
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
