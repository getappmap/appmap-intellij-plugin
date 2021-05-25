package appland.actions;

import appland.Icons;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class AppMapDocumentationAction extends AnAction implements DumbAware {
    public AppMapDocumentationAction() {
        super(Icons.APPMAP_DOCS);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        BrowserUtil.browse("https://appland.com/docs");
    }
}
