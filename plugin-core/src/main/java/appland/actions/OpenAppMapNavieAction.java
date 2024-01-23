package appland.actions;

import appland.webviews.appMap.NavieEditorProvider;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import org.jetbrains.annotations.NotNull;

/**
 * Action to open the AppMap Navie webview.
 */
@SuppressWarnings("ComponentNotRegistered")
public class OpenAppMapNavieAction extends AnAction implements DumbAware {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        var project = e.getProject();
        if (project != null) {
            NavieEditorProvider.openEditor(e.getProject(), e.getDataContext());
        }
    }
}
