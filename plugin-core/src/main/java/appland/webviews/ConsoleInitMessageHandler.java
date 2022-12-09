package appland.webviews;

import com.intellij.openapi.diagnostic.Logger;
import org.cef.CefSettings;
import org.cef.browser.CefBrowser;
import org.cef.handler.CefDisplayHandlerAdapter;
import org.jetbrains.annotations.NotNull;

/**
 * Shared handler to initialize the JCEF webview as soon as the client JS appliation sent "intellij-plugin-ready" to
 * the webview host.
 * This class is also an adapter of JS application's console logging to the IDE's logging framework.
 */
public final class ConsoleInitMessageHandler extends CefDisplayHandlerAdapter {
    private static final Logger LOG = Logger.getInstance(ConsoleInitMessageHandler.class);

    private final @NotNull Runnable onInit;

    public static final String READY_MESSAGE_ID = "intellij-plugin-ready";

    public ConsoleInitMessageHandler(@NotNull Runnable onInit) {
        this.onInit = onInit;
    }

    @Override
    public boolean onConsoleMessage(CefBrowser browser, CefSettings.LogSeverity level, String message, String source, int line) {
        if (READY_MESSAGE_ID.equals(message)) {
            onInit.run();
            return true;
        }

        var output = String.format("AppMap JS, %s:%d, %s", source, line, message);
        switch (level) {
            case LOGSEVERITY_FATAL:
                LOG.error(output);
                return true;
            // logging ERROR as warning because the AppMap app always logs errors about svg image dimensions at start
            case LOGSEVERITY_ERROR:
            case LOGSEVERITY_WARNING:
                LOG.warn(output);
                return true;
            case LOGSEVERITY_INFO:
                LOG.info(output);
                return true;
            default:
                LOG.debug(output);
                return true;
        }
    }
}
