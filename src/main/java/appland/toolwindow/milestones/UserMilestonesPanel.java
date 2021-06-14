package appland.toolwindow.milestones;

import com.intellij.ui.components.panels.VerticalLayout;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.LinkedList;
import java.util.List;

/**
 * Manages a list of collapsible user milestone panels.
 */
public class UserMilestonesPanel extends JPanel {
    private final List<CollapsibleUserMilestonePanel> collapsiblePanels = new LinkedList<>();

    public UserMilestonesPanel() {
        super(new VerticalLayout(5));

        addPanel("Quickstart", new UserMilestoneContentPanel() {
            @Override
            protected void setupPanel() {
                add(new StatusLabel(UserMilestoneStatus.Success, "Install AppMap extension"));
                add(new StatusLabel(UserMilestoneStatus.Failed, "Setup AppMap agent"));
                add(new StatusLabel(UserMilestoneStatus.Pending, "Create AppMaps"));
                add(new StatusLabel(UserMilestoneStatus.Pending, "View your AppMaps"));
            }
        });
        addPanel("Using AppMaps", new UserMilestoneContentPanel() {
            @Override
            protected void setupPanel() {
                add(new StatusLabel(UserMilestoneStatus.Pending, "First step"));
                add(new StatusLabel(UserMilestoneStatus.Pending, "Second step"));
            }
        });
        addPanel("Mastermind AppsMaps", new UserMilestoneContentPanel() {
            @Override
            protected void setupPanel() {
                add(new StatusLabel(UserMilestoneStatus.Pending, "First step"));
                add(new StatusLabel(UserMilestoneStatus.Pending, "Second step"));
            }
        });
        addPanel("Support", new UserMilestoneContentPanel() {
            @Override
            protected void setupPanel() {
                add(new StatusLabel(UserMilestoneStatus.Pending, "First step"));
                add(new StatusLabel(UserMilestoneStatus.Pending, "Second step"));
            }
        });

        // demo purpose
        collapsiblePanels.get(0).setNotificationCount(1);
    }

    private void addPanel(@NotNull String title, @NotNull UserMilestoneContentPanel panel) {
        var collapsiblePanel = new CollapsibleUserMilestonePanel(panel, true, title);
        collapsiblePanels.add(collapsiblePanel);
        add(collapsiblePanel);

        // collapse all other panels when a panel was expanded
        collapsiblePanel.addCollapsingListener((collapsedPanel, isCollapsed) -> {
            if (!isCollapsed) {
                for (CollapsibleUserMilestonePanel p : collapsiblePanels) {
                    if (!collapsedPanel.equals(p)) {
                        p.setCollapsed(true);
                    }
                }
            }
        }, null);
    }
}
