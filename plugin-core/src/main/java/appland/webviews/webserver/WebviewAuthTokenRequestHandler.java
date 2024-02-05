package appland.webviews.webserver;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Urls;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefRequestHandlerAdapter;
import org.cef.handler.CefResourceRequestHandler;
import org.cef.handler.CefResourceRequestHandlerAdapter;
import org.cef.misc.BoolRef;
import org.cef.network.CefRequest;
import org.jetbrains.builtInWebServer.BuiltInWebServerKt;
import org.jetbrains.ide.BuiltInServerManager;

/**
 * Adds auth tokens to requests to webview resources to bypass any filtering of the built-in webserver.
 * Without auth tokens the IDE's built-in webserver only accepts URLs prefixed with a project name.
 */
public final class WebviewAuthTokenRequestHandler extends CefRequestHandlerAdapter {
    @Override
    public CefResourceRequestHandler getResourceRequestHandler(CefBrowser browser,
                                                               CefFrame frame,
                                                               CefRequest request,
                                                               boolean isNavigation,
                                                               boolean isDownload,
                                                               String requestInitiator,
                                                               BoolRef disableDefaultHandling) {

        if (isUnsignedWebViewRequest(request)) {
            return new CefResourceRequestHandlerAdapter() {
                @Override
                public boolean onBeforeResourceLoad(CefBrowser browser, CefFrame frame, CefRequest request) {
                    var url = Urls.parseEncoded(request.getURL());
                    if (url != null) {
                        request.setURL(BuiltInServerManager.getInstance().addAuthToken(url).toExternalForm());
                    }

                    // false to continue with the request
                    return false;
                }
            };
        }

        // fallback to default handling of JCEF
        return null;
    }

    /**
     * @return true if the request is for a webview asset, but not yet signed with an auth token
     */
    private static boolean isUnsignedWebViewRequest(CefRequest request) {
        var url = Urls.parseEncoded(request.getURL());
        var params = StringUtil.defaultIfEmpty(url != null ? url.getParameters() : null, "");
        return request.getURL().startsWith(AppMapWebview.getBaseUrlWithPath())
                && !params.contains("?" + BuiltInWebServerKt.TOKEN_PARAM_NAME + "=")
                && !params.contains("&" + BuiltInWebServerKt.TOKEN_PARAM_NAME + "=");
    }
}
