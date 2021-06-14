package appland.toolwindow.milestones;

import com.intellij.icons.AllIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * Status of a single milestone step.
 */
enum UserMilestoneStatus {
    Pending, Success, Failed;

    @NotNull
    public Icon getIcon() {
        switch (this) {
            case Success:
                return AllIcons.Actions.Checked;
            case Failed:
                return AllIcons.General.Error;
            case Pending:
                return AllIcons.General.Gear;
            default:
                throw new IllegalStateException("unknown status: " + this);
        }
    }
}
