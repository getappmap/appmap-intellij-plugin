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
     * @return {@code true} if a JSON-RPC process was launched for the given project and is still owned by the
     * service. Unlike {@link AppLandJsonRpcService#isServerRunning()}, this is already {@code true} while the
     * process is starting up, i.e. before it announced its port.
     */
    public static boolean hasJsonRpcProcess(@NotNull Project project) {
        var service = (DefaultAppLandJsonRpcService) AppLandJsonRpcService.getInstance(project);
        synchronized (service) {
            return service.currentProcess != null;
        }
    }

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
