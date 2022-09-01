package appland.installGuide;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.DumbAware;
import org.jetbrains.annotations.NotNull;

public class OpenDevToolsAction extends AnAction implements DumbAware {
    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(ApplicationManager.getApplication().isInternal());
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        var editor = FileEditorManager.getInstance(e.getProject()).getSelectedEditor();
        if (editor instanceof InstallGuideEditor) {
            ((InstallGuideEditor)editor).openDevTools();
        }
    }
}
