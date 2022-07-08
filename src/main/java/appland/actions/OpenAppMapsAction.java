package appland.actions;

import appland.installGuide.InstallGuideViewPage;
import appland.installGuide.InstallGuideEditorProvider;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class OpenAppMapsAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        InstallGuideEditorProvider.open(Objects.requireNonNull(e.getProject()), InstallGuideViewPage.InstallGuide);
    }
}
