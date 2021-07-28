package appland.actions;

import appland.milestones.UserMilestonesEditorProvider;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;

public class OpenUserMilestonesAction extends AnAction {
    private static final Logger LOG = Logger.getInstance("#appmap.action");

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        UserMilestonesEditorProvider.openUserMilestones(e.getProject());
    }
}
