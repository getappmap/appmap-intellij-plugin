package appland.webviews.webserver;

import appland.AppMapPlugin;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.builtInWebServer.BuiltInServerOptions;

import java.nio.file.Path;

import static appland.webviews.webserver.AppMapWebviewRequestHandler.APPMAP_SERVER_BASE_PATH;

/**
 * Webviews available in the AppMap plugin.
 */
@RequiredArgsConstructor
@Getter
public enum AppMapWebview {
    AppMap("appmap"),
    InstallGuide("appland-install-guide"),
    Findings("appland-findings"),
    SignIn("appland-signin");

    public static @NotNull String getBaseUrl() {
        return "http://localhost:" + BuiltInServerOptions.getInstance().getEffectiveBuiltInServerPort();
    }

    private final @NotNull String webviewAssetsDirectoryName;

    /**
     * @return Directory on the local filesystem, where this webview's data is stored.
     */
    public @NotNull Path getBaseDirPath() {
        return AppMapPlugin.getPluginPath().resolve(webviewAssetsDirectoryName);
    }

    /**
     * @return HTTP URL of the IDE's built-in webserver for this webview's index.html file.
     */
    public @NotNull String getIndexHtmlUrl() {
        return getBaseUrl() + APPMAP_SERVER_BASE_PATH + "/" + webviewAssetsDirectoryName + "/index.html";
    }
}
