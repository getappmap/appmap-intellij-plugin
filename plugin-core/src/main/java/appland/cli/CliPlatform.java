package appland.cli;

import com.intellij.openapi.util.SystemInfo;
import com.intellij.util.system.CpuArch;
import org.jetbrains.annotations.NotNull;

public final class CliPlatform {

    private CliPlatform() {
    }

    /**
     * @return The platform identifier string formatted for manifest mapping (e.g., "linux-x64", "macos-arm64", "win-x64").
     */
    public static @NotNull String getId() {
        return currentPlatform() + "-" + currentArch();
    }

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

    public static @NotNull String currentArch() {
        return CpuArch.isArm64() ? "arm64" : "x64";
    }
}
