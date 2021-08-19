package appland.startup;

import appland.milestones.UserMilestonesEditorProvider;
import appland.settings.AppMapApplicationSettingsService;
import appland.toolwindow.AppMapToolWindowFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.wm.ToolWindowManager;
import org.jetbrains.annotations.NotNull;

public class AppLandStartupActivity implements StartupActivity {
    @Override
    public void runActivity(@NotNull Project project) {
        if (AppMapApplicationSettingsService.getInstance().isFirstStart()) {
            AppMapApplicationSettingsService.getInstance().setFirstStart(false);

            openToolWindowAndQuickstart(project);
        }
    }

    static void openToolWindowAndQuickstart(@NotNull Project project) {
        StartupManager.getInstance(project).runWhenProjectIsInitialized(() -> {
            var window = ToolWindowManager.getInstance(project).getToolWindow(AppMapToolWindowFactory.TOOL_WINDOW_ID);
            if (window != null) {
                window.show(() -> {
                    UserMilestonesEditorProvider.openUserQuickstart(project);
                });
            }
        });
    }
}
