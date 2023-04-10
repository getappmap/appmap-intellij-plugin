package appland.toolwindow;

import appland.settings.AppMapApplicationSettingsService;
import appland.settings.AppMapSettingsListener;
import appland.toolwindow.signInView.SignInViewPanel;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * Factory to create AppMap-mode tool windows.
 * <p>
 * It switches the content between a sign-in panel and the AppMap content panels, depending on the current
 * state of user authentication.
 */
public class AppMapToolWindowFactory implements ToolWindowFactory, DumbAware {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        updateToolWindowContent(project, toolWindow);

        // update the tool window after the AppMap API key changed
        ApplicationManager.getApplication().getMessageBus()
                .connect(toolWindow.getDisposable())
                .subscribe(AppMapSettingsListener.TOPIC, new AppMapSettingsListener() {
                    @Override
                    public void apiKeyChanged() {
                        ApplicationManager.getApplication().invokeLater(() -> {
                            toolWindow.getContentManager().removeAllContents(true);
                            updateToolWindowContent(project, toolWindow);
                        }, ModalityState.defaultModalityState());
                    }
                });
    }

    private void updateToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        var panel = createContentPanel(project, toolWindow);
        project.getMessageBus().connect(panel).subscribe(ToolWindowManagerListener.TOPIC, new ToolWindowManagerListener() {
            @Override
            public void stateChanged(@NotNull ToolWindowManager toolWindowManager) {
                if (toolWindow.isVisible()) {
                    panel.onToolWindowShown();
                } else {
                    panel.onToolWindowHidden();
                }
            }
        });

        assert panel instanceof JPanel;
        var content = ContentFactory.SERVICE.getInstance().createContent(null, null, false);
        content.setComponent((JPanel) panel);
        toolWindow.getContentManager().addContent(content);

        if (toolWindow.isVisible()) {
            panel.onToolWindowShown();
        }
    }

    @NotNull
    private AppMapToolWindowContent createContentPanel(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        return AppMapApplicationSettingsService.getInstance().isAuthenticated()
                ? new AppMapWindowPanel(project, toolWindow.getDisposable())
                : new SignInViewPanel(toolWindow.getDisposable());
    }
}

