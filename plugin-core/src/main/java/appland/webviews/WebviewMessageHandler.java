package appland.webviews;

import com.google.gson.JsonObject;
import com.intellij.ui.jcef.JBCefJSQuery;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Handler of message sent by the webview JS application to the plugin host,
 * which is usually an editor using a JCEF panel.
 */
@FunctionalInterface
public interface WebviewMessageHandler {
    @Nullable
    JBCefJSQuery.Response handleWebviewMessage(@NotNull String messageId, @Nullable JsonObject message);
}
