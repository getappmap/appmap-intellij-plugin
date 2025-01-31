package appland.rpcService;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public interface AppLandJsonRpcListener {
    Topic<AppLandJsonRpcListener> TOPIC = Topic.create("appmap.jsonRpcListener", AppLandJsonRpcListener.class);

    default void serverStarted() {
    }

    default void serverStopped() {
    }

    /**
     * Sent before the JSON-RPC server is restarted.
     */
    default void beforeServerRestart() {
    }

    /**
     * Sent after the JSON-RPC server was restarted.
     */
    default void serverRestarted() {
    }

    /**
     * Sent after a new AppMap JSON-RPC server was fully initialized.
     *
     * @param contentRoots      The content roots passed to the server process
     * @param appMapConfigFiles The content files passed to the server process
     */
    default void serverConfigurationUpdated(@NotNull Collection<VirtualFile> contentRoots,
                                            @NotNull Collection<VirtualFile> appMapConfigFiles) {
    }
}
