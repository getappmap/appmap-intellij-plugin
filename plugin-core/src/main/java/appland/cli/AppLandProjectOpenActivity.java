package appland.cli;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;

public class AppLandProjectOpenActivity implements StartupActivity, DumbAware {
    @Override
    public void runActivity(@NotNull Project project) {
        // We're not launching the CLI processes by default in test mode,
        // because it's async and may interfere with other tests.
        if (ApplicationManager.getApplication().isUnitTestMode()) {
            return;
        }

        AppLandCommandLineService.getInstance().refreshForOpenProjectsInBackground();
    }
}
