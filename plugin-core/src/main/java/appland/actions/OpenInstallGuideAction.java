package appland.actions;

import appland.installGuide.InstallGuideEditorProvider;
import appland.installGuide.InstallGuideViewPage;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class OpenInstallGuideAction extends AnAction implements DumbAware {
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(InstallGuideEditorProvider.isSupported());
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        InstallGuideEditorProvider.open(Objects.requireNonNull(e.getProject()), InstallGuideViewPage.InstallAgent);
    }
}
