package appland.cli;

import appland.AppMapBundle;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * The different CLI tools that can be run.
 * See {@link CliTools} for helper methods to find binaries of a given type.
 */
@Getter
public enum CliTool {
    AppMap("appmap", AppMapBundle.get("cli.appMap.name")),
    Scanner("scanner", AppMapBundle.get("cli.scanner.name"));

    private final String id;
    private final String presentableName;

    CliTool(@NotNull String id, String presentableName) {
        this.id = id;
        this.presentableName = presentableName;
    }

    /**
     * @param platform "linux", "macos", or "win"
     * @param arch     "x64" or "arm64"
     * @return The name of the binary for the given platform and architecture.
     * The name is used locally.
     */
    public @NotNull String getBinaryName(@NotNull String platform, @NotNull String arch) {
        var suffix = "win".equals(platform) ? ".exe" : "";
        return id + "-" + platform + "-" + arch + suffix;
    }
}
