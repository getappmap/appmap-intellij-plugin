package appland.toolwindow;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

/**
 * Factory to create AppMap-mode tool windows.
 */
public class AppMapToolWindowFactory implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        var content = ContentFactory.SERVICE.getInstance().createContent(null, null, false);
        var panel = new AppMapWindowPanel(project, content);
        /* content.setComponent(DumbService.getInstance(project).wrapWithSpoiler(panel, () -> {
            ApplicationManager.getApplication().invokeLater(panel::rebuild, project.getDisposed());
        }, panel)); */
        content.setComponent(panel);
        toolWindow.getContentManager().addContent(content);
    }
}

