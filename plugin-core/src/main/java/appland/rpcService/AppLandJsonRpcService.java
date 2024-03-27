package appland.rpcService;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Project service to interact with the AppLand JSON-RPC service, mainly used for Navie.
 */
public interface AppLandJsonRpcService extends Disposable {
    static @NotNull AppLandJsonRpcService getInstance(@NotNull Project project) {
        return project.getService(AppLandJsonRpcService.class);
    }

    boolean isServerRunning();

    void startServer();

    void stopServer();

    @Nullable
    Integer getServerPort();
}
