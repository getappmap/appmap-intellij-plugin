package appland.cli;

import appland.deployment.AppMapDeploymentSettingsService;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.util.system.CpuArch;
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
     * @return The path to the binary, if it exists.
     * All matching bundled binaries and a downloaded binary (if it exists) are searched.
     * The binary with the highest version number is returned.
     * If both a bundled binary and the downloaded binary have the highest version, the bundled binary is returned.
     */
    public static @Nullable Path getBinaryPath(@NotNull CliTool type) {
        return getBinaryPath(type, currentPlatform(), currentArch());
    }

    /**
     * @return The path to the binary, if it exists.
     * All matching bundled binaries and a downloaded binary (if it exists) are searched.
     * The binary with the highest version number is returned.
     * If both a bundled binary and the downloaded binary have the highest version, the bundled binary is returned.
     */
    public static @Nullable Path getBinaryPath(@NotNull CliTool type, @NotNull String platform, @NotNull String arch) {
        var downloadedBinary = AppLandDownloadService.getInstance().getDownloadFilePath(type, platform, arch);
        var bestBundledBinary = AppMapDeploymentSettingsService.getInstance()
                .findBundledBinaries(type, platform, arch)
                .filter(Files::exists)
                .max(pathComparator)
                .orElse(null);

        if (bestBundledBinary == null) {
            return downloadedBinary;
        } else if (downloadedBinary == null) {
            return bestBundledBinary;
        }

        // == 0: same version, prefer the bundled binary
        // < 0: the downloaded binary has a lower version, prefer the bundled binary
        int compareValue = pathComparator.compare(downloadedBinary, bestBundledBinary);
        var selectedBinary = compareValue == 0 || compareValue < 0
                ? bestBundledBinary
                : downloadedBinary;

        try {
            fixBinaryPermissions(selectedBinary);
            return selectedBinary;
        } catch (IOException e) {
            LOG.warn("Failed to make the binary executable. Path: " + selectedBinary, e);
            return null;
        }
    }

    public static final @NotNull Comparator<Path> pathComparator = Comparator.comparing(
            path -> extractVersion(path.toString()),
            SemVerComparator.INSTANCE);

    /**
     * package-visible for our tests
     */
    public static @NotNull String currentPlatform() {
        if (SystemInfo.isLinux) {
            return "linux";
        }

        if (SystemInfo.isMac) {
            return "macos";
        }

        if (SystemInfo.isWindows) {
            return "win";
        }

        throw new IllegalStateException("Unsupported platform: " + SystemInfo.getOsNameAndVersion());
    }

    /**
     * package-visible for our tests
     */
    public static @NotNull String currentArch() {
        return CpuArch.isArm64() ? "arm64" : "x64";
    }

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
        if (SystemInfo.isUnix && !Files.isExecutable(downloadTargetFilePath)) {
            Files.setPosixFilePermissions(downloadTargetFilePath, Set.of(OWNER_READ, OWNER_WRITE, OWNER_EXECUTE, GROUP_READ, OTHERS_READ));
        }
    }
}
