package appland.startup;

import appland.ProjectActivityAdapter;
import appland.problemsView.FindingsManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;

public class AppLandStartupActivity extends ProjectActivityAdapter {
    @Override
    public void runActivity(@NotNull Project project) {
        var application = ApplicationManager.getApplication();

        // Don't reload in unit tests because the reload is async.
        // AppMapBaseTest reset all findings before each test, anyway.
        if (application.isUnitTestMode()) {
            return;
        }

        // load initial findings of the project
        application.executeOnPooledThread(() -> {
            ReadAction.run(() -> {
                FindingsManager.getInstance(project).reloadAsync();
            });
        });
    }
}
