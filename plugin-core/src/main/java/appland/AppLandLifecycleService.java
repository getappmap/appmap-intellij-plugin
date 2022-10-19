package appland;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class AppLandLifecycleService implements Disposable {
    public static @NotNull Disposable getInstance() {
        return ApplicationManager.getApplication().getService(AppLandLifecycleService.class);
    }

    public static @NotNull Disposable getInstance(@NotNull Project project) {
        return project.getService(AppLandLifecycleService.class);
    }

    @Override
    public void dispose() {
    }
}
