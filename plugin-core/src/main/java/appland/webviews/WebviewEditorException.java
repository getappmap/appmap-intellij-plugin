package appland.webviews;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

/**
 * Exception thrown if a {@link WebviewEditor} failed to initialize.
 * The exception provides an error message, suitable to be shown to the user.
 */
public final class WebviewEditorException extends Exception {
    public WebviewEditorException(@Nls String message) {
        super(message);
    }

    public WebviewEditorException(@Nls String message, @NotNull Throwable cause) {
        super(message, cause);
    }
}
