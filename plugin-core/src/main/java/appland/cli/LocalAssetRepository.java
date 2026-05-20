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
import java.util.Comparator;

public final class LocalAssetRepository {
    private static final Logger LOG = Logger.getInstance(LocalAssetRepository.class);

    private LocalAssetRepository() {
    }

    private static final String ACTIVE_VERSION_FILE = "active-version.txt";

    /**
     * Returns the platform-specific cache directory for downloaded CLI binaries.
     * Matches the location used by the VSCode AppMap extension so both plugins share cached downloads.
     */
    public static @NotNull Path getCacheDirectory(boolean unitTestMode) {
        if (unitTestMode || "true".equals(System.getProperty("appmap.sandbox"))) {
            return Paths.get(PathManager.getTempPath()).resolve("appland-downloads");
        }
        var home = Paths.get(System.getProperty("user.home"));
        if (SystemInfo.isWindows) {
            var localAppData = System.getenv("LOCALAPPDATA");
            return localAppData != null
                    ? Paths.get(localAppData, "AppMap", "cache")
                    : home.resolve("AppData").resolve("Local").resolve("AppMap").resolve("cache");
        } else if (SystemInfo.isMac) {
            return home.resolve("Library").resolve("Caches").resolve("AppMap");
        } else {
            var xdgCacheHome = System.getenv("XDG_CACHE_HOME");
            return xdgCacheHome != null
                    ? Paths.get(xdgCacheHome).resolve("appmap")
                    : home.resolve(".cache").resolve("appmap");
        }
    }

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
        var platform = CliPlatform.currentPlatform();
        var arch = CliPlatform.currentArch();

        var activeVersion = getActiveVersion(type, unitTestMode);
        if (activeVersion != null && isDownloaded(type, activeVersion, platform, arch, unitTestMode)) {
            return activeVersion;
        }

        return findHighestCachedVersion(type, platform, arch, unitTestMode);
    }

    private static @Nullable String findHighestCachedVersion(@NotNull CliTool type,
                                                             @NotNull String platform,
                                                             @NotNull String arch,
                                                             boolean unitTestMode) {
        var cacheDir = getCacheDirectory(unitTestMode);
        var prefix = type.getId() + "-" + platform + "-" + arch + "-";
        try (var stream = Files.list(cacheDir)) {
            return stream
                    .filter(p -> isExecutableBinary(p))
                    .map(p -> p.getFileName().toString())
                    .filter(name -> name.startsWith(prefix))
                    .map(name -> {
                        var stripped = name.endsWith(".exe") ? name.substring(0, name.length() - 4) : name;
                        return stripped.substring(prefix.length());
                    })
                    .filter(v -> !v.isEmpty() && Version.parseVersion(v) != null)
                    .max(Comparator.naturalOrder())
                    .orElse(null);
        } catch (IOException e) {
            LOG.debug("Error scanning cache directory: " + cacheDir, e);
            return null;
        }
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

    /**
     * Returns the path where a specific version of the binary is cached.
     * Binaries are stored flat in the shared cache directory with the version embedded in the filename,
     * matching the layout used by the VSCode AppMap extension.
     */
    public static @NotNull Path getExecutableFilePath(@NotNull CliTool type,
                                                      @NotNull String version,
                                                      @NotNull String platform,
                                                      @NotNull String arch,
                                                      boolean unitTestMode) {
        return getCacheDirectory(unitTestMode).resolve(type.getBinaryName(platform, arch, version));
    }

    /**
     * Returns a per-tool subdirectory of the cache used for plugin-specific metadata (e.g. active-version.txt).
     */
    public static @NotNull Path getToolDownloadDirectory(@NotNull CliTool type, boolean unitTestMode) {
        return getCacheDirectory(unitTestMode).resolve(type.getId());
    }
}
