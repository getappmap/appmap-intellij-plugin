package appland.webviews;

import com.intellij.ui.jcef.JBCefBrowserBase;
import org.cef.handler.CefLoadHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Shared, generic handler for navigation errors inside a webview.
 */
public final class DefaultWebviewErrorPage implements JBCefBrowserBase.ErrorPage {
    private final @NotNull AtomicBoolean isNavigating;

    public DefaultWebviewErrorPage(@NotNull AtomicBoolean isNavigating) {
        this.isNavigating = isNavigating;
    }

    @Override
    public @Nullable String create(@NotNull CefLoadHandler.ErrorCode errorCode,
                                   @NotNull String errorText,
                                   @NotNull String failedUrl) {
        if (errorCode == CefLoadHandler.ErrorCode.ERR_ABORTED && isNavigating.getAndSet(false)) {
            return null;
        }
        return JBCefBrowserBase.ErrorPage.DEFAULT.create(errorCode, errorText, failedUrl);
    }
}
