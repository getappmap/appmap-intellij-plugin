package appland.cli;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;

/**
 * Notifies when the JSON-RPC service of an AppMap indexer is available.
 */
@FunctionalInterface
public interface AppLandIndexerJsonRpcListener {
    @Topic.AppLevel
    Topic<AppLandIndexerJsonRpcListener> TOPIC = Topic.create("appland.indexer.jsonRpc", AppLandIndexerJsonRpcListener.class);

    void indexerServiceAvailable(@NotNull VirtualFile directory);
}
