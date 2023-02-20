package appland.toolwindow;

import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;

/**
 * A collapsible panel with a title (always visible) and content (visible only if expanded).
 */
public class CollapsiblePanel extends JPanel {
    private final Collection<CollapsingListener> listeners = ContainerUtil.createLockFreeCopyOnWriteList();
    private final JComponent content;
    private final CollapsiblePanelTitle title;
    private final boolean growVertically;
    private boolean isCollapsed;
    private boolean isInitialized = false;

    public CollapsiblePanel(@NotNull String title, boolean isCollapsed, @NotNull JComponent content, boolean growVertically) {
        super(new BorderLayout());

        this.content = content;
        this.growVertically = growVertically;

        this.title = new CollapsiblePanelTitle(title, isCollapsed);
        this.title.addLabelActionListener(() -> setCollapsed(!isCollapsed()));
        add(this.title, BorderLayout.NORTH);

        setFocusable(false);
        setCollapsed(isCollapsed);
    }

    public boolean isCollapsed() {
        return isCollapsed;
    }

    protected void setCollapsed(boolean collapse) {
        try {
            var maxSize = getMaximumSize();
            var maxWidth = maxSize != null ? maxSize.width : Integer.MAX_VALUE;
            var maxHeight = maxSize != null ? maxSize.height : Integer.MAX_VALUE;
            var verticalInsets = content.getHeight() + getInsets().top + getInsets().bottom;

            if (!collapse) {
                add(content, BorderLayout.CENTER);
                maxHeight = growVertically
                        ? Integer.MAX_VALUE
                        : title.getHeight() + verticalInsets;
            } else if (isInitialized) {
                remove(content);
                maxHeight = title.getHeight() + verticalInsets;
            }

            setMaximumSize(new Dimension(maxWidth, maxHeight));

            isCollapsed = collapse;
            title.setCollapsed(isCollapsed);

            notifyListeners();

            revalidate();
            repaint();
        } finally {
            isInitialized = true;
        }
    }

    private void notifyListeners() {
        for (CollapsingListener listener : listeners) {
            listener.onCollapsingChanged(this, isCollapsed());
        }
    }

    interface CollapsingListener {
        void onCollapsingChanged(@NotNull CollapsiblePanel panel, boolean isCollapsed);
    }
}