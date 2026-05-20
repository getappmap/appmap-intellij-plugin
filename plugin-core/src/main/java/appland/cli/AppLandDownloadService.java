package appland.cli;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * Application service managing the downloads of CLI binaries.
 */
public interface AppLandDownloadService {
    static @NotNull AppLandDownloadService getInstance() {
        return ApplicationManager.getApplication().getService(AppLandDownloadService.class);
    }

    /**
     * Starts tasks to download the latest available versions of the needed CLI binaries.
     */
    void queueDownloadTasks(@NotNull Project project);
}
