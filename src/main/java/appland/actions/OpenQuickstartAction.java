package appland.actions;

import appland.milestones.MilestonesViewType;
import appland.milestones.UserMilestonesEditorProvider;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class OpenQuickstartAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        UserMilestonesEditorProvider.open(Objects.requireNonNull(e.getProject()), MilestonesViewType.InstallAgent);
    }
}
