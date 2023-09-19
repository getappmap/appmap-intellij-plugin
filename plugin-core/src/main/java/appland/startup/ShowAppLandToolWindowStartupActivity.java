package appland.startup;

import appland.settings.AppMapApplicationSettingsService;
import appland.telemetry.TelemetryService;
import appland.toolwindow.AppMapToolWindowFactory;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.util.EmptyRunnable;
import com.intellij.openapi.wm.ToolWindowManager;
import org.jetbrains.annotations.NotNull;

public class ShowAppLandToolWindowStartupActivity implements StartupActivity.DumbAware {
    private static void showAppMapToolWindow(@NotNull Project project) {
        StartupManager.getInstance(project).runAfterOpened(() -> ApplicationManager.getApplication().invokeLater(() -> {
            var toolWindow = ToolWindowManager.getInstance(project).getToolWindow(AppMapToolWindowFactory.TOOLWINDOW_ID);
            if (toolWindow != null && !toolWindow.isVisible()) {
                toolWindow.activate(EmptyRunnable.getInstance(), false);
            }
        }, ModalityState.defaultModalityState()));
    }

    private static void sendTelemetry(@NotNull Project project) {
        var telemetry = TelemetryService.getInstance();
        telemetry.notifyTelemetryUsage(project);
        telemetry.sendEvent("plugin:install");
    }

    @Override
    public void runActivity(@NotNull Project project) {
        handleFirstStart(project);
    }

    /**
     * If it's the first launch of the AppLand plugin, it opens the AppMap toolwindow and sends telemetry.
     */
    static void handleFirstStart(@NotNull Project project) {
        var settings = AppMapApplicationSettingsService.getInstance();
        if (settings.isFirstStart() && !ApplicationManager.getApplication().isUnitTestMode()) {
            settings.setFirstStart(false);
            showAppMapToolWindow(project);
            sendTelemetry(project);
        }
    }
}
