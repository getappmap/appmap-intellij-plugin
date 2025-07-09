package appland.webviews.webserver;

import appland.AppMapPlugin;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
    InstallGuide("install-guide"),
    Findings("findings"),
    Navie("navie"),
    Review("review");

    public static @NotNull String getBaseUrl() {
        return "http://localhost:" + BuiltInServerOptions.getInstance().getEffectiveBuiltInServerPort();
    }

    public static @NotNull String getBaseUrlWithPath() {
        return getBaseUrl() + APPMAP_SERVER_BASE_PATH;
    }

    public static @NotNull String getBaseUrlWithPath(@NotNull String path) {
        return getBaseUrlWithPath() + "/" + path;
    }

    private final @NotNull String name;

    /**
     * @return HTTP URL of the IDE's built-in webserver for this webview's index.html file.
     */
    public @NotNull String getIndexHtmlUrl() {
        return getBaseUrlWithPath() + "/" + name + ".html";
    }
}
