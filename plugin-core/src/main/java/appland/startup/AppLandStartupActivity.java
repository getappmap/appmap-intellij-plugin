package appland.startup;

import appland.problemsView.FindingsManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;

public class AppLandStartupActivity implements StartupActivity {
    @Override
    public void runActivity(@NotNull Project project) {
        // Don't reload in unit tests because the reload is async.
        // AppMapBaseTest reset all findings before each test, anyway.
        if (ApplicationManager.getApplication().isUnitTestMode()) {
            return;
        }

        // load initial findings of the project
        FindingsManager.getInstance(project).reloadAsync();
    }
}
