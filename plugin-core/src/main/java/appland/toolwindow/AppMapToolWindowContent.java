package appland.toolwindow;

import com.intellij.openapi.Disposable;

/**
 * Content panels of the AppMap tool window.
 */
public interface AppMapToolWindowContent extends Disposable {
    void onToolWindowShown();

    void onToolWindowHidden();
}
