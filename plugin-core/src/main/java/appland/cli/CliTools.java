package appland.cli;

import appland.deployment.AppMapDeploymentSettingsService;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.text.SemVer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Set;
import java.util.regex.Pattern;

import static java.nio.file.attribute.PosixFilePermission.*;

public final class CliTools {
    private static final Logger LOG = Logger.getInstance(CliTools.class);
    private static final @NotNull Pattern semVerPattern = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)(?:-(\\w+))?");

    private CliTools() {
    }

    /**
     * @return {@code true} if a binary is available for the given type, either downloaded or bundled.
     */
    public static boolean isBinaryAvailable(@NotNull CliTool type) {
        return CliTools.getBinaryPath(type) != null;
    }

    /**
     * @return The path to the binary for the current platform, if it exists.
     * The installed binary (symlink or Windows copy in ~/.appmap/bin) is preferred.
     * Falls back to the best matching bundled binary.
     */
    public static @Nullable Path getBinaryPath(@NotNull CliTool type) {
        return getBinaryPath(type, CliPlatform.currentPlatform(), CliPlatform.currentArch());
    }

    /**
     * @return The path to the binary, if it exists.
     * The installed binary (symlink or Windows copy in ~/.appmap/bin) is preferred.
     * Falls back to the best matching bundled binary for the given platform and arch.
     */
    public static @Nullable Path getBinaryPath(@NotNull CliTool type, @NotNull String platform, @NotNull String arch) {
        var installedBinary = LocalAssetRepository.getInstalledBinaryPath(type);
        if (installedBinary != null) {
            try {
                fixBinaryPermissions(installedBinary);
                return installedBinary;
            } catch (IOException e) {
                LOG.warn("Failed to make the installed binary executable. Path: " + installedBinary, e);
            }
        }

        // Fall back to the best bundled binary when nothing has been downloaded yet.
        var bestBundledBinary = AppMapDeploymentSettingsService.getInstance()
                .findBundledBinaries(type, platform, arch)
                .filter(Files::exists)
                .max(pathComparator)
                .orElse(null);

        if (bestBundledBinary == null) {
            return null;
        }

        try {
            fixBinaryPermissions(bestBundledBinary);
            return bestBundledBinary;
        } catch (IOException e) {
            LOG.warn("Failed to make the bundled binary executable. Path: " + bestBundledBinary, e);
            return null;
        }
    }

    public static final @NotNull Comparator<Path> pathComparator = Comparator.comparing(
            path -> extractVersion(path.toString()),
            SemVerComparator.INSTANCE);

    /**
     * @return The first SemVer match in the given string, or {@code null} if there is no match.
     */
    public static @Nullable SemVer extractVersion(@NotNull String value) {
        var matcher = semVerPattern.matcher(value);
        if (matcher.find()) {
            return SemVer.parseFromText(value.substring(matcher.start(), matcher.end()));
        }
        return null;
    }

    /**
     * On Unix systems, if the file is not yet executable, set the permissions of the given file to 744.
     * On Windows, do nothing.
     */
    public static void fixBinaryPermissions(Path downloadTargetFilePath) throws IOException {
        if (com.intellij.openapi.util.SystemInfo.isUnix && !Files.isExecutable(downloadTargetFilePath)) {
            Files.setPosixFilePermissions(downloadTargetFilePath, Set.of(OWNER_READ, OWNER_WRITE, OWNER_EXECUTE, GROUP_READ, OTHERS_READ));
        }
    }
}

