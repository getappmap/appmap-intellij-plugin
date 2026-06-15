package appland.cli;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.SystemInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.util.text.SemVer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;

public final class LocalAssetRepository {
    private static final Logger LOG = Logger.getInstance(LocalAssetRepository.class);

    private LocalAssetRepository() {
    }

    private static boolean isSandboxOrTestMode(boolean unitTestMode) {
        return unitTestMode || "true".equals(System.getProperty("appmap.sandbox"));
    }

    /**
     * Returns the platform-specific cache directory for downloaded CLI binaries.
     * Matches the location used by the VSCode AppMap extension so both plugins share cached downloads.
     * In unit-test mode a stable per-user directory is used so downloaded binaries survive across
     * test invocations and only need to be fetched once per version per machine.
     */
    public static @NotNull Path getCacheDirectory(boolean unitTestMode) {
        if (unitTestMode) {
            var xdgCacheHome = System.getenv("XDG_CACHE_HOME");
            var cacheBase = xdgCacheHome != null
                    ? Paths.get(xdgCacheHome)
                    : Paths.get(System.getProperty("user.home"), ".cache");
            return cacheBase.resolve("appmap-test");
        }
        if ("true".equals(System.getProperty("appmap.sandbox"))) {
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

    /**
     * Returns the directory that holds current-version symlinks.
     * In production: ~/.appmap/bin (matching the VSCode AppMap extension).
     * In test/sandbox mode: a sandboxed temp directory to avoid polluting the real bin dir.
     * Note: unlike getCacheDirectory, this intentionally uses the combined sandbox+unitTest check
     * because the bin dir must always be ephemeral in both modes — tests call removeDownloads() to
     * reset symlink state between runs, and runIde sandbox must not touch the real ~/.appmap/bin.
     */
    public static @NotNull Path getBinDirectory() {
        if (isSandboxOrTestMode(ApplicationManager.getApplication().isUnitTestMode())) {
            return Paths.get(PathManager.getTempPath()).resolve("appland-bin");
        }
        return Paths.get(System.getProperty("user.home"), ".appmap", "bin");
    }

    /**
     * Returns the path of the current-version symlink (or Windows copy fallback) for the given tool,
     * e.g. ~/.appmap/bin/appmap on Unix or ~/.appmap/bin/appmap.exe on Windows.
     */
    public static @NotNull Path getSymlinkPath(@NotNull CliTool type) {
        var name = type.getId() + (SystemInfo.isWindows ? ".exe" : "");
        return getBinDirectory().resolve(name);
    }

    /**
     * Creates or updates the symlink at {@code symlinkPath} pointing to {@code cachedPath}.
     * Falls back to copying the file on systems where symlinks are not supported (e.g. locked-down Windows).
     */
    public static void updateSymlink(@NotNull Path cachedPath, @NotNull Path symlinkPath) {
        try {
            Files.deleteIfExists(symlinkPath);
        } catch (IOException e) {
            LOG.debug("Error removing existing symlink: " + symlinkPath, e);
        }
        try {
            Files.createDirectories(symlinkPath.getParent());
        } catch (IOException e) {
            LOG.warn("Failed to create bin directory: " + symlinkPath.getParent(), e);
            return;
        }
        try {
            Files.createSymbolicLink(symlinkPath, cachedPath);
        } catch (IOException | UnsupportedOperationException | SecurityException e) {
            // Fallback: copy the file (e.g. Windows without symlink privileges)
            try {
                Files.copy(cachedPath, symlinkPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                CliTools.fixBinaryPermissions(symlinkPath);
            } catch (IOException ex) {
                LOG.warn("Failed to create symlink or copy binary at " + symlinkPath, ex);
            }
        }
    }

    /**
     * @return true if the symlink already points to {@code targetPath}, or if it is a regular file
     * (Windows copy fallback — treat as correct to avoid redundant re-copies on steady-state runs).
     */
    public static boolean symlinkPointsTo(@NotNull Path symlinkPath, @NotNull Path targetPath) {
        try {
            if (Files.isSymbolicLink(symlinkPath)) {
                var link = Files.readSymbolicLink(symlinkPath);
                return targetPath.equals(symlinkPath.getParent().resolve(link));
            }
            return Files.isRegularFile(symlinkPath);
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Extracts the version from a bundled binary filename by splitting on the last '-'.
     * e.g. "appmap-linux-x64-v1.2.3" → "1.2.3"
     * Does NOT handle prerelease versions (e.g. "1.2.3-rc.1") — use
     * {@link #versionFromCachedFilename} for downloaded cache files where the prefix is known.
     */
    public static @Nullable String versionFromPath(@NotNull Path path) {
        var filename = path.getFileName().toString();
        if (filename.endsWith(".exe")) {
            filename = filename.substring(0, filename.length() - 4);
        }
        var lastDash = filename.lastIndexOf('-');
        if (lastDash < 0) return null;
        var after = filename.substring(lastDash + 1);
        // Tolerate an optional leading "v" (e.g. bundled binaries with v1.2.3 suffix)
        if (after.startsWith("v")) after = after.substring(1);
        return (!after.isEmpty() && Character.isDigit(after.charAt(0))) ? after : null;
    }

    /**
     * Extracts the version from a cached binary filename by stripping the known prefix.
     * Unlike {@link #versionFromPath}, this correctly handles prerelease versions
     * (e.g. "appmap-linux-x64-1.2.3-rc.1" → "1.2.3-rc.1").
     */
    private static @Nullable String versionFromCachedFilename(@NotNull String filename, @NotNull String prefix) {
        if (filename.endsWith(".exe")) {
            filename = filename.substring(0, filename.length() - 4);
        }
        if (!filename.startsWith(prefix)) return null;
        var version = filename.substring(prefix.length());
        if (version.startsWith("v")) version = version.substring(1);
        return (!version.isEmpty() && Character.isDigit(version.charAt(0))) ? version : null;
    }

    /**
     * Returns the installed version by reading the symlink target.
     * Returns null if not installed or if the version cannot be determined (e.g. Windows copy fallback).
     */
    public static @Nullable String getInstalledVersion(@NotNull CliTool type) {
        var symlinkPath = getSymlinkPath(type);
        if (Files.isSymbolicLink(symlinkPath)) {
            try {
                var target = Files.readSymbolicLink(symlinkPath);
                var filename = symlinkPath.getParent().resolve(target).getFileName().toString();
                var prefix = type.getId() + "-" + CliPlatform.currentPlatform() + "-" + CliPlatform.currentArch() + "-";
                return versionFromCachedFilename(filename, prefix);
            } catch (IOException e) {
                LOG.debug("Error reading symlink for version: " + symlinkPath, e);
            }
        }
        // Not a symlink (Windows copy fallback) or read failed: version unknown
        return null;
    }

    /**
     * Returns the symlink (or copy) path for the tool if it exists and is executable, null otherwise.
     * isExecutableBinary follows symlinks, so this works for both symlinks and Windows copy fallbacks.
     */
    public static @Nullable Path getInstalledBinaryPath(@NotNull CliTool type) {
        var symlinkPath = getSymlinkPath(type);
        return isExecutableBinary(symlinkPath) ? symlinkPath : null;
    }

    /**
     * Resolves a symlink to its target so the version embedded in the target filename can be
     * used for comparison. Returns the path as-is if it is not a symlink.
     */
    public static @Nullable Path resolveForVersionComparison(@Nullable Path path) {
        if (path == null) return null;
        if (Files.isSymbolicLink(path)) {
            try {
                return path.toRealPath();
            } catch (IOException e) {
                LOG.debug("Error resolving symlink for version comparison: " + path, e);
            }
        }
        return path;
    }

    /**
     * Returns the highest-version executable binary in the cache for the given tool/platform/arch,
     * or null if none is found. Used to restore the symlink when the manifest is unreachable.
     */
    public static @Nullable Path findHighestCachedBinary(@NotNull CliTool type,
                                                          @NotNull String platform,
                                                          @NotNull String arch,
                                                          boolean unitTestMode) {
        return findHighestCachedBinaryIn(type, platform, arch, getCacheDirectory(unitTestMode));
    }

    /**
     * Like {@link #findHighestCachedBinary} but searches an explicit directory.
     * Used by tests that need an isolated cache dir so they don't pollute the shared test cache.
     */
    static @Nullable Path findHighestCachedBinaryIn(@NotNull CliTool type,
                                                     @NotNull String platform,
                                                     @NotNull String arch,
                                                     @NotNull Path cacheDir) {
        var prefix = type.getId() + "-" + platform + "-" + arch + "-";
        try (var stream = Files.list(cacheDir)) {
            return stream
                    .filter(LocalAssetRepository::isExecutableBinary)
                    .filter(p -> p.getFileName().toString().startsWith(prefix))
                    .max(Comparator.comparing(p -> {
                        var v = versionFromCachedFilename(p.getFileName().toString(), prefix);
                        return v != null ? SemVer.parseFromText(v) : null;
                    }, SemVerComparator.INSTANCE))
                    .orElse(null);
        } catch (IOException e) {
            LOG.debug("Error scanning cache directory for fallback binary: " + cacheDir, e);
            return null;
        }
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
     * Returns the cache directory for the given tool (both tools share the same flat cache directory).
     */
    public static @NotNull Path getToolDownloadDirectory(@NotNull CliTool type, boolean unitTestMode) {
        return getCacheDirectory(unitTestMode);
    }
}
