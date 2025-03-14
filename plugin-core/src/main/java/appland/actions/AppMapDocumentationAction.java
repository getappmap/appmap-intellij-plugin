package appland.actions;

import appland.Icons;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import org.jetbrains.annotations.NotNull;

public class AppMapDocumentationAction extends AnAction implements DumbAware {
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    public AppMapDocumentationAction() {
        super(Icons.APPMAP_DOCS);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        BrowserUtil.browse("https://appmap.io/docs");
    }
}
