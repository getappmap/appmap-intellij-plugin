package appland.cli;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import org.jetbrains.annotations.NotNull;

/**
 * Stops CLI processes of opened and closed projects.
 */
public class AppLandProjectManagerListener implements ProjectManagerListener {
    @Override
    public void projectOpened(@NotNull Project project) {
        AppLandCommandLineService.getInstance().refreshForOpenProjectsInBackground();
    }

    @Override
    public void projectClosed(@NotNull Project project) {
        AppLandCommandLineService.getInstance().refreshForOpenProjectsInBackground();
    }
}
