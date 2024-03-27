package appland.rpcService;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;

public class AppMapJsonRpcServerStartupActivity implements StartupActivity, DumbAware {
    @Override
    public void runActivity(@NotNull Project project) {
        if (!project.isDefault()) {
            AppLandJsonRpcService.getInstance(project).startServer();
        }
    }
}
