package appland.cli;

import appland.AppMapBundle;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

public enum CliTool {
    AppMap("appmap",
            "https://github.com/getappmap/appmap-js/releases/download/%40appland%2Fappmap",
            "https://registry.npmjs.org/@appland%2Fappmap/latest",
            AppMapBundle.get("cli.appMap.name")),
    Scanner("scanner",
            "https://github.com/getappmap/appmap-js/releases/download/%40appland%2Fscanner",
            "https://registry.npmjs.org/@appland%2Fscanner/latest",
            AppMapBundle.get("cli.scanner.name"));

    @Getter
    private final String id;
    @Getter
    private final String baseURL;
    @Getter
    private final String latestVersionURL;
    @Getter
    private final String presentableName;

    CliTool(@NotNull String id, @NotNull String baseURL, String latestVersionURL, String presentableName) {
        this.id = id;
        this.baseURL = baseURL;
        this.latestVersionURL = latestVersionURL;
        this.presentableName = presentableName;
    }

    public @NotNull String getBinaryName(@NotNull String platform, @NotNull String arch) {
        return id + "-" + platform + "-" + arch;
    }

    public @NotNull String getDownloadUrl(@NotNull String version, @NotNull String platform, @NotNull String arch) {
        return baseURL + "-v" + version + "/" + getBinaryName(platform, arch);
    }
}
