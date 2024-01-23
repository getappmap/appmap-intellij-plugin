package appland.webviews.webserver;

import appland.AppMapPlugin;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/**
 * Webviews available in the AppMap plugin.
 */
@RequiredArgsConstructor
@Getter
public enum AppMapWebview {
    AppMap("appmap"),
    InstallGuide("appland-install-guide"),
    Findings("appland-findings"),
    SignIn("appland-signin"),
    Navie("appland-navie");

    private final @NotNull String webviewAssetsDirectoryName;

    public @NotNull Path getBaseDir() {
        return AppMapPlugin.getPluginPath().resolve(webviewAssetsDirectoryName);
    }
}
