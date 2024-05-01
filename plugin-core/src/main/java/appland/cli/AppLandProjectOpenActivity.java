package appland.cli;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;

public class AppLandProjectOpenActivity implements StartupActivity, DumbAware {
    @Override
    public void runActivity(@NotNull Project project) {
        AppLandCommandLineService.getInstance().refreshForOpenProjectsInBackground();
    }
}
