package appland.webviews;

import com.google.gson.JsonObject;
import com.intellij.ui.jcef.JBCefJSQuery;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Simplified message handler, which returns "success" by default and an error response when an exception occurred.
 */
public interface SimpleWebviewMessageHandler extends WebviewMessageHandler {
    /**
     * @return {@code true} if the message was handled, {@code false} if the message was not handled.
     * @throws Exception If an error occurred while handling the message
     */
    boolean handleMessage(@NotNull String messageId, @Nullable JsonObject message) throws Exception;

    @Override
    default @Nullable JBCefJSQuery.Response handleWebviewMessage(@NotNull String messageId, @Nullable JsonObject message) {
        try {
            if (handleMessage(messageId, message)) {
                return new JBCefJSQuery.Response("success");
            }
            return null;
        } catch (Exception e) {
            return new JBCefJSQuery.Response("failure", 1, e.getMessage());
        }
    }
}
