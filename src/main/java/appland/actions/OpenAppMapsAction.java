package appland.actions;

import appland.milestones.UserMilestonesEditorProvider;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class OpenAppMapsAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        UserMilestonesEditorProvider.openUserAppMaps(Objects.requireNonNull(e.getProject()));
    }
}
