package appland.rpcService;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;
import java.util.Set;

/**
 * Project service to interact with the AppLand JSON-RPC service, mainly used for Navie.
 * The lifecycle of the server process is bound to the project.
 * It's automatically terminated when the project is closed.
 */
public interface AppLandJsonRpcService extends Disposable {
    static @NotNull AppLandJsonRpcService getInstance(@NotNull Project project) {
        return project.getService(AppLandJsonRpcService.class);
    }

    // all environment variables, which define settings of Navie
    Set<String> LLM_ENV_VARIABLES = Set.of(
            "OPENAI_API_KEY",
            "OPENAI_BASE_URL",
            "APPMAP_NAVIE_MODEL",
            "APPMAP_NAVIE_TOKEN_LIMIT",
            "AZURE_OPENAI_API_KEY",
            "AZURE_OPENAI_API_VERSION",
            "AZURE_OPENAI_API_INSTANCE_NAME",
            "AZURE_OPENAI_API_DEPLOYMENT_NAME"
    );

    /**
     * @return {@code true} if the JSON-RPC server is running, regardless if the port was already printed on STDOUT.
     */
    boolean isServerRunning();

    /**
     * Starts the server on the current thread.
     */
    void startServer();

    /**
     * Stops the server in a background thread.
     * The calling thread is not blocked.
     */
    void stopServerAsync();

    /**
     * Stops the server on the current thread.
     * @param timeout The timeout to wait for process termination. 0 disables the waiting.
     * @param timeUnit Unit of the timeout value
     */
    void stopServerSync(int timeout, @NotNull TimeUnit timeUnit);

    /**
     * @return The port, as returned by the launched JSON-RPC server.
     * {@code null} is returned if the server is not running or has not printed the used port on STDOUT.
     */
    @Nullable
    Integer getServerPort();
}
