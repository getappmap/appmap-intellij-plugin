package appland.toolwindow.milestones;

import com.intellij.ui.components.panels.VerticalLayout;
import com.intellij.util.ui.JBUI;

import javax.swing.*;

abstract class UserMilestoneContentPanel extends JPanel {
    UserMilestoneContentPanel() {
        super(new VerticalLayout(0));
        setBorder(JBUI.Borders.empty(5, 15, 5, 5));
        setupPanel();
    }

    protected abstract void setupPanel();
}
