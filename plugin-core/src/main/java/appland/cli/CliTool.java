package appland.cli;

import appland.AppMapBundle;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public enum CliTool {
    AppMap("appmap",
            "https://github.com/getappmap/appmap-js/releases/download/@appland/appmap",
            "https://registry.npmjs.org/@appland/appmap/latest",
            AppMapBundle.get("cli.appMap.name")),
    Scanner("scanner",
            "https://github.com/getappmap/appmap-js/releases/download/@appland/scanner",
            "https://registry.npmjs.org/@appland/scanner/latest",
            AppMapBundle.get("cli.scanner.name"));

    private final String id;
    private final String baseURL;
    private final String latestVersionURL;
    private final String presentableName;

    CliTool(@NotNull String id, @NotNull String baseURL, String latestVersionURL, String presentableName) {
        this.id = id;
        this.baseURL = baseURL;
        this.latestVersionURL = latestVersionURL;
        this.presentableName = presentableName;
    }

    public @NotNull String getBinaryName(@NotNull String platform, @NotNull String arch) {
        var suffix = "win".equals(platform) ? ".exe" : "";
        return id + "-" + platform + "-" + arch + suffix;
    }

    public @NotNull String getDownloadUrl(@NotNull String version, @NotNull String platform, @NotNull String arch) {
        return baseURL + "-v" + version + "/" + getBinaryName(platform, arch);
    }
}
