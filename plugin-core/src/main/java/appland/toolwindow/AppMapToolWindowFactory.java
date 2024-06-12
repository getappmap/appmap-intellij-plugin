package appland.toolwindow;

import appland.notifications.AppMapNotifications;
import appland.settings.AppMapApplicationSettingsService;
import appland.settings.AppMapSettingsListener;
import appland.toolwindow.appmap.AppMapWindowPanel;
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
import com.intellij.ui.jcef.JBCefApp;
import com.intellij.util.concurrency.annotations.RequiresEdt;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * Factory to create AppMap-mode tool windows.
 * <p>
 * It switches the content between a sign-in panel and the AppMap content panels, depending on the current
 * state of user authentication.
 */
public class AppMapToolWindowFactory implements ToolWindowFactory, DumbAware {
    // must match the value in plugin.xml
    public static final String TOOLWINDOW_ID = "applandToolWindow";

    /**
     * Open the AppMap tool window and put focus in the AppMap section.
     *
     * @param project Current project
     */
    @RequiresEdt
    public static void showAppMapTreePanel(@NotNull Project project) {
        var toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TOOLWINDOW_ID);
        if (toolWindow != null) {
            toolWindow.activate(() -> {
                var content = toolWindow.getContentManager().getContent(0);
                if (content != null && content.getComponent() instanceof AppMapWindowPanel) {
                    ((AppMapWindowPanel) content.getComponent()).showAppMapTreePanel();
                }
            }, true, true);
        }
    }

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

        if (AppMapNotifications.isWebviewProxyWarningRequired()) {
            AppMapNotifications.showWebviewProxyBrokenWarning(project);
        }
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

        var content = ContentFactory.getInstance().createContent(null, null, false);
        content.setComponent(panel);
        toolWindow.getContentManager().addContent(content);

        if (toolWindow.isVisible()) {
            panel.onToolWindowShown();
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends JPanel & AppMapToolWindowContent> @NotNull T createContentPanel(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        if (!JBCefApp.isSupported()) {
            return (T) new JcefUnsupportedPanel();
        }

        return AppMapApplicationSettingsService.getInstance().isAuthenticated()
                ? (T) new AppMapWindowPanel(project, toolWindow.getDisposable())
                : (T) new SignInViewPanel(toolWindow.getDisposable());
    }
}

