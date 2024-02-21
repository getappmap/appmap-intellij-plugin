package appland.webviews.webserver;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.EmptyHttpHeaders;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.ide.HttpRequestHandler;
import org.jetbrains.io.FileResponses;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Request handler to server AppMap's webview files through the IDE's built-in webserver.
 */
public class AppMapWebviewRequestHandler extends HttpRequestHandler {
    static final String APPMAP_SERVER_BASE_PATH = "/_appmap-webviews";

    @Override
    public boolean process(@NotNull QueryStringDecoder queryStringDecoder,
                           @NotNull FullHttpRequest fullHttpRequest,
                           @NotNull ChannelHandlerContext channelHandlerContext) throws IOException {
        String requestPath;
        try {
            requestPath = queryStringDecoder.path();
        } catch (IllegalArgumentException e) {
            // https://github.com/getappmap/appmap-intellij-plugin/issues/579, for example
            return false;
        }

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
                var baseDirPath = webview.getBaseDirPath();
                var filePath = baseDirPath.resolve(relativeFilePath).toAbsolutePath().normalize();
                return filePath.startsWith(baseDirPath) ? filePath : null;
            }
        }

        return null;
    }
}
