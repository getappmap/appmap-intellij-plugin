package appland.rpcService;

import com.intellij.execution.process.KillableProcessHandler;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

@TestOnly
public class TestAppLandJsonRpcService extends DefaultAppLandJsonRpcService {
    public TestAppLandJsonRpcService(@NotNull Project project) {
        super(project);
    }

    /**
     * Terminates the JSON-RPC process of the given project, if it's currently running.
     * @param project Project
     */
    public static void killJsonRpcProcess(@NotNull Project project) {
        var service = (DefaultAppLandJsonRpcService) AppLandJsonRpcService.getInstance(project);
        KillableProcessHandler process;
        synchronized (service) {
            process = service.currentProcess;
        }
        if (process != null) {
            process.killProcess();
        }
    }
}
