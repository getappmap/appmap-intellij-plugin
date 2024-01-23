package appland.webviews.webserver;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Url;
import com.intellij.util.Urls;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.EmptyHttpHeaders;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.builtInWebServer.BuiltInServerOptions;
import org.jetbrains.ide.HttpRequestHandler;
import org.jetbrains.io.FileResponses;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Request handler to server AppMap's webview files through the IDE's built-in webserver.
 */
public class AppMapWebviewRequestHandler extends HttpRequestHandler {
    private static final String APPMAP_SERVER_BASE_PATH = "/_appmap-webviews";

    /**
     * @param webview      Selects the webview to server
     * @param relativePath Relative file inside the webview's data directory
     * @return HTTP URL of the IDE's built-in webserver which is serving the data for the webviews file.
     */
    public static @NotNull Url getWebviewUrl(@NotNull AppMapWebview webview, @NotNull String relativePath) {
        var host = "localhost:" + BuiltInServerOptions.getInstance().getEffectiveBuiltInServerPort();
        var trimmedRelativePath = StringUtil.trimStart(relativePath, "/");
        var urlPath = APPMAP_SERVER_BASE_PATH + "/" + webview.getWebviewAssetsDirectoryName() + "/" + trimmedRelativePath;
        return Urls.newHttpUrl(host, urlPath);
    }

    @Override
    public boolean process(@NotNull QueryStringDecoder queryStringDecoder,
                           @NotNull FullHttpRequest fullHttpRequest,
                           @NotNull ChannelHandlerContext channelHandlerContext) throws IOException {
        var requestPath = queryStringDecoder.path();
        if (!requestPath.startsWith(APPMAP_SERVER_BASE_PATH + "/")) {
            return false;
        }

        var webviewPath = requestPath.substring(APPMAP_SERVER_BASE_PATH.length() + 1);
        var slashIndex = webviewPath.indexOf('/');
        if (slashIndex == -1) {
            return false;
        }

        var webviewDirName = webviewPath.substring(0, slashIndex);
        var relativeFilePath = slashIndex + 1 < webviewPath.length() ? webviewPath.substring(slashIndex + 1) : null;
        if (relativeFilePath == null) {
            return false;
        }

        var localFile = findLocalWebviewFile(webviewDirName, relativeFilePath);
        if (localFile == null) {
            return false;
        }

        FileResponses.INSTANCE.sendFile(fullHttpRequest,
                channelHandlerContext.channel(),
                localFile,
                EmptyHttpHeaders.INSTANCE);
        return true;
    }

    private @Nullable Path findLocalWebviewFile(@NotNull String webviewDirName, @NotNull String relativeFilePath) {
        for (var webview : AppMapWebview.values()) {
            if (webview.getWebviewAssetsDirectoryName().equals(webviewDirName)) {
                var baseDirPath = webview.getBaseDir();
                var filePath = baseDirPath.resolve(relativeFilePath).toAbsolutePath().normalize();
                return filePath.startsWith(baseDirPath) ? filePath : null;
            }
        }

        return null;
    }
}
