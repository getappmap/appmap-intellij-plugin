package appland.webviews;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Urls;
import com.intellij.util.io.URLUtil;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefRequestHandlerAdapter;
import org.cef.network.CefRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Request handler to open external links a user clicked on in the system's native browser.
 */
public class OpenExternalLinksHandler extends CefRequestHandlerAdapter {
    @Override
    public boolean onBeforeBrowse(CefBrowser browser,
                                  CefFrame frame,
                                  CefRequest request,
                                  boolean user_gesture,
                                  boolean is_redirect) {
        // JavaDoc says: "True to cancel the navigation or false to continue."
        return user_gesture && openExternalLink(request.getURL());
    }

    /**
     * Open the given URL in an external browser if it's a http:// or https:// link.
     *
     * @param url URL to open
     * @return {@code true} if the link was opened in an external window.
     */
    public static boolean openExternalLink(@Nullable String url) {
        if (url != null && isExternalUrl(url)) {
            BrowserUtil.browse(url);
            return true;
        }
        return false;
    }

    public static boolean isExternalUrl(@NotNull String url) {
        var parsed = Urls.parseEncoded(url);
        if (parsed == null) {
            return false;
        }

        var scheme = parsed.getScheme();
        var host = parsed.getAuthority();
        host = host != null && host.contains(":") ? host.substring(0, host.indexOf(':')) : host;

        var isValidProtocol = URLUtil.HTTP_PROTOCOL.equalsIgnoreCase(scheme)
                || URLUtil.HTTPS_PROTOCOL.equalsIgnoreCase(scheme)
                || "mailto".equalsIgnoreCase(scheme);
        var isValidHost = !StringUtil.equalsIgnoreCase(host, "localhost")
                && !StringUtil.equalsIgnoreCase(host, "127.0.0.1");
        return isValidProtocol && isValidHost;
    }
}
