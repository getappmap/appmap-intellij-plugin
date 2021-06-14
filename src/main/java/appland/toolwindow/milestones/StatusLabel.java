package appland.toolwindow.milestones;

import com.intellij.ui.components.JBLabel;
import org.jetbrains.annotations.NotNull;

/**
 * Label representing a single item of user milestones, e.g. "Install AppMap extension".
 */
class StatusLabel extends JBLabel {
    private UserMilestoneStatus status;

    StatusLabel(@NotNull UserMilestoneStatus status, @NotNull String label) {
        super(label, status.getIcon(), LEADING);
        this.status = status;
    }

    void setStatus(UserMilestoneStatus status) {
        this.status = status;
        setIcon(status.getIcon());
    }
}
