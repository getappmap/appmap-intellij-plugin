package appland.rpcService;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    String OPENAI_API_KEY = "OPENAI_API_KEY";
    String AZURE_OPENAI_API_KEY = "AZURE_OPENAI_API_KEY";
    // all environment variables, which define LLM settings of Navie
    Set<String> LLM_ENV_VARIABLES = Set.of(
            OPENAI_API_KEY,
            "OPENAI_BASE_URL",
            "APPMAP_NAVIE_MODEL",
            "APPMAP_NAVIE_TOKEN_LIMIT",
            AZURE_OPENAI_API_KEY,
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
     * @return The port, as returned by the launched JSON-RPC server.
     * {@code null} is returned if the server is not running or has not printed the used port on STDOUT.
     */
    @Nullable
    Integer getServerPort();
}
