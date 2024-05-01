package appland.startup;

import appland.problemsView.FindingsManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;

public class AppLandStartupActivity implements StartupActivity {
    @Override
    public void runActivity(@NotNull Project project) {
        // load initial findings of the project
        FindingsManager.getInstance(project).reloadAsync();
    }
}
