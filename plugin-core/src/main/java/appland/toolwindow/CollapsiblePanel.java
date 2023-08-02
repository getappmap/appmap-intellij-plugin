package appland.toolwindow;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;

/**
 * A collapsible panel with a title (always visible) and content (visible only if expanded).
 */
public class CollapsiblePanel extends JPanel {
    private final @NotNull Collection<CollapsingListener> listeners = ContainerUtil.createLockFreeCopyOnWriteList();
    private final @NotNull JComponent content;
    private final @NotNull CollapsiblePanelTitle title;
    private boolean isCollapsed;

    public CollapsiblePanel(@NotNull Project project,
                            @NotNull String title,
                            @NotNull String collapsedPropertyKey,
                            boolean isCollapsedDefault,
                            @NotNull JComponent content) {
        super(new BorderLayout());

        this.content = content;

        this.title = new CollapsiblePanelTitle(title, isCollapsedDefault);
        this.title.addLabelActionListener(() -> {
            setCollapsed(!isCollapsed());
            PropertiesComponent.getInstance(project).setValue(collapsedPropertyKey, isCollapsed, isCollapsedDefault);
        });
        add(this.title, BorderLayout.NORTH);

        setFocusable(false);
        setCollapsed(PropertiesComponent.getInstance(project).getBoolean(collapsedPropertyKey, isCollapsedDefault));
    }

    @Override
    public void doLayout() {
        super.doLayout();

        if (getParent() != null) {
            updateCollapsedSize();
        }
    }

    public boolean isCollapsed() {
        return isCollapsed;
    }

    protected void setCollapsed(boolean collapse) {
        if (collapse) {
            remove(content);
        } else {
            add(content, BorderLayout.CENTER);
        }

        isCollapsed = collapse;
        title.setCollapsed(isCollapsed);

        if (getParent() != null) {
            updateCollapsedSize();
        }

        notifyListeners();

        revalidate();
        repaint();
    }

    private void updateCollapsedSize() {
        if (isCollapsed) {
            var insets = getInsets();
            var dimension = new Dimension(Integer.MAX_VALUE, title.getHeight() + insets.top + insets.bottom);
            setMaximumSize(dimension);
        } else {
            setMaximumSize(null);
        }
    }

    private void notifyListeners() {
        for (var listener : listeners) {
            listener.onCollapsingChanged(this, isCollapsed());
        }
    }

    interface CollapsingListener {
        void onCollapsingChanged(@NotNull CollapsiblePanel panel, boolean isCollapsed);
    }
}