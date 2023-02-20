package appland.toolwindow;

import com.intellij.ui.components.panels.VerticalLayout;
import com.intellij.util.ui.JBUI;

import javax.swing.*;

public abstract class AppMapContentPanel extends JPanel {
    public AppMapContentPanel(boolean initialize) {
        super(new VerticalLayout(0));
        setBorder(JBUI.Borders.empty(5, 15, 5, 5));
        if (initialize) {
            setupPanel();
        }
    }

    protected abstract void setupPanel();
}
