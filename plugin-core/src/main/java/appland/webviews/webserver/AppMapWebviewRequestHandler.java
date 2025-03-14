package appland.webviews.webserver;

import com.intellij.openapi.application.ReadAction;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.ide.HttpRequestHandler;
import org.jetbrains.io.FileResponses;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Request handler to server AppMap's webview files through the IDE's built-in webserver.
 */
public class AppMapWebviewRequestHandler extends HttpRequestHandler {
    static final String APPMAP_SERVER_BASE_PATH = "/_appmap-webviews";
    static final String APPMAP_IDE_STYLES_PATH = "/_appmap-webviews/ide-styles.css";

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

        // The IDE styles are served with path /_appmap-webviews/ide-styles.css,
        // the styles are computed on demand
        if (APPMAP_IDE_STYLES_PATH.equals(requestPath)) {
            return processIdeStyles(fullHttpRequest, channelHandlerContext);
        }

        return processWebviewResource(fullHttpRequest, channelHandlerContext, requestPath);
    }

    private boolean processIdeStyles(@NotNull FullHttpRequest fullHttpRequest, @NotNull ChannelHandlerContext channelHandlerContext) {
        var isHeadRequest = Objects.equals(fullHttpRequest.method(), HttpMethod.HEAD);
        var channel = channelHandlerContext.channel();

        var response = FileResponses.INSTANCE.prepareSend(fullHttpRequest,
                channel,
                System.currentTimeMillis(),
                "ide-styles.css");
        if (response == null) {
            return false;
        }

        var ideStyles = ReadAction.compute(IdeStyleRequest::createIdeStyles);
        var ideStylesBytes = ideStyles.getBytes(StandardCharsets.UTF_8);

        if (!isHeadRequest) {
            HttpUtil.setContentLength(response, ideStylesBytes.length);
        }
        channel.write(response);

        if (!isHeadRequest) {
            channel.write(Unpooled.copiedBuffer(ideStylesBytes));
        }

        var cf = channel.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
        cf.addListener(ChannelFutureListener.CLOSE);
        return true;
    }

    private boolean processWebviewResource(@NotNull FullHttpRequest fullHttpRequest, @NotNull ChannelHandlerContext channelHandlerContext, String requestPath) {
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
