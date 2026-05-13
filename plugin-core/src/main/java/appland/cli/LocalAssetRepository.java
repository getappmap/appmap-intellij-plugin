package appland.cli;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.Version;
import com.intellij.openapi.util.io.NioFiles;
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

    public static @Nullable String findLatestDownloadedVersion(@NotNull CliTool type) {
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

    public static void removeOtherVersions(@NotNull CliTool type, @NotNull String keepVersion, boolean unitTestMode) {
        removeOtherVersions(getToolDownloadDirectory(type, unitTestMode), keepVersion);
    }

    public static @NotNull List<Path> removeOtherVersions(@NotNull Path directory, @NotNull String keepVersion) {
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
