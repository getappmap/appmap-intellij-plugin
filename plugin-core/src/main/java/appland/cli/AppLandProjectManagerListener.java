package appland.cli;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import org.jetbrains.annotations.NotNull;

/**
 * Stops CLI processes of opened and closed projects.
 * {@link AppLandProjectOpenActivity} will start the CLI processes for opened projects.
 */
public class AppLandProjectManagerListener implements ProjectManagerListener {
    @Override
    public void projectClosed(@NotNull Project project) {
        // We're not launching the CLI processes by default in test mode,
        // because it's async and may interfere with other tests.
        if (ApplicationManager.getApplication().isUnitTestMode()) {
            return;
        }

        AppLandCommandLineService.getInstance().refreshForOpenProjectsInBackground();
    }
}
