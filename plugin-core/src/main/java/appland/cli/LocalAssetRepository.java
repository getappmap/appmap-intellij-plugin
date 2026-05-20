package appland.cli;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.Version;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public final class LocalAssetRepository {
    private static final Logger LOG = Logger.getInstance(LocalAssetRepository.class);

    private LocalAssetRepository() {
    }

    private static final String ACTIVE_VERSION_FILE = "active-version.txt";

    public static @Nullable String getActiveVersion(@NotNull CliTool type, boolean unitTestMode) {
        var directory = getToolDownloadDirectory(type, unitTestMode);
        var activeVersionFile = directory.resolve(ACTIVE_VERSION_FILE);
        if (Files.isRegularFile(activeVersionFile)) {
            try {
                var version = Files.readString(activeVersionFile).trim();
                if (!version.isEmpty()) {
                    return version;
                }
            } catch (IOException e) {
                LOG.debug("Error reading active version file: " + activeVersionFile, e);
            }
        }
        return null;
    }

    public static void setActiveVersion(@NotNull CliTool type, @NotNull String version, boolean unitTestMode) {
        var directory = getToolDownloadDirectory(type, unitTestMode);
        try {
            Files.createDirectories(directory);
            Files.writeString(directory.resolve(ACTIVE_VERSION_FILE), version);
        } catch (IOException e) {
            LOG.warn("Failed to write active version file for " + type, e);
        }
    }

    /**
     * @return The active version if its binary exists on disk, or the highest available fallback version.
     */
    public static @Nullable String getInstalledVersion(@NotNull CliTool type) {
        var unitTestMode = ApplicationManager.getApplication().isUnitTestMode();
        var directory = getToolDownloadDirectory(type, unitTestMode);
        if (!Files.isDirectory(directory)) {
            return null;
        }

        var activeVersion = getActiveVersion(type, unitTestMode);
        if (activeVersion != null && isDownloaded(type, activeVersion, CliPlatform.currentPlatform(), CliPlatform.currentArch(), unitTestMode)) {
            return activeVersion;
        }

        var candidates = findVersionDownloadDirectories(directory);
        if (candidates.isEmpty()) {
            return null;
        }

        var platform = CliPlatform.currentPlatform();
        var arch = CliPlatform.currentArch();

        return candidates
                .stream()
                .map(file -> file.getFileName().toString())
                .filter(v -> isDownloaded(type, v, platform, arch, unitTestMode))
                .max(Comparator.naturalOrder())
                .orElse(null);
    }

    public static @Nullable Path getInstalledBinaryPath(@NotNull CliTool type, @NotNull String platform, @NotNull String arch) {
        var unitTestMode = ApplicationManager.getApplication().isUnitTestMode();
        var version = getInstalledVersion(type);
        return version != null ? getExecutableFilePath(type, version, platform, arch, unitTestMode) : null;
    }

    public static boolean isDownloaded(@NotNull CliTool type, @NotNull String version, @NotNull String platform, @NotNull String arch, boolean unitTestMode) {
        return isExecutableBinary(getExecutableFilePath(type, version, platform, arch, unitTestMode));
    }

    public static boolean isExecutableBinary(@NotNull Path file) {
        return Files.isRegularFile(file) && (!SystemInfo.isUnix || Files.isExecutable(file));
    }

    public static @NotNull List<Path> findVersionDownloadDirectories(@NotNull Path parentDirectory) {
        try (var stream = Files.list(parentDirectory)) {
            return stream
                    .filter(file -> Files.isDirectory(file) && Version.parseVersion(file.getFileName().toString()) != null)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            LOG.debug("Error listing download directory entries: " + parentDirectory, e);
            return Collections.emptyList();
        }
    }

    public static @NotNull Path getExecutableFilePath(@NotNull CliTool type,
                                               @NotNull String version,
                                               @NotNull String platform,
                                               @NotNull String arch,
                                               boolean unitTestMode) {
        return getVersionDownloadDirectory(type, version, unitTestMode).resolve(type.getBinaryName(platform, arch));
    }

    public static @NotNull Path getVersionDownloadDirectory(@NotNull CliTool type, @NotNull String version, boolean unitTestMode) {
        return getToolDownloadDirectory(type, unitTestMode).resolve(version);
    }

    public static @NotNull Path getToolDownloadDirectory(@NotNull CliTool type, boolean unitTestMode) {
        var basePath = unitTestMode || "true".equals(System.getProperty("appmap.sandbox"))
                ? Paths.get(PathManager.getTempPath()).resolve("appland-downloads")
                : Paths.get(PathManager.getDefaultSystemPathFor("appland-plugin"));
        return basePath.resolve(type.getId());
    }
}
