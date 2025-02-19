package appland.cli;

import appland.ProjectActivityAdapter;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class AppLandProjectOpenActivity extends ProjectActivityAdapter implements DumbAware {
    @Override
    public void runActivity(@NotNull Project project) {
        // In 2024.2, StartupActivity (but not ProjectActivity) is not synchronous anymore for tests.
        // We're enforcing this for all versions to make tests more reliable
        // and to avoid surprises when we migrate to ProjectActivity.
        if (!ApplicationManager.getApplication().isUnitTestMode()) {
            AppLandCommandLineService.getInstance().refreshForOpenProjectsInBackground();
        }
    }
}
