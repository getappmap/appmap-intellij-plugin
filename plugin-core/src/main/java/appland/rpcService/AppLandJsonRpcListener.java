package appland.rpcService;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public interface AppLandJsonRpcListener {
    Topic<AppLandJsonRpcListener> TOPIC = Topic.create("appmap.jsonRpcListener", AppLandJsonRpcListener.class);

    void serverStarted();

    void serverStopped();

    void serverRestarted();

    void serverConfigurationUpdated(@NotNull Collection<VirtualFile> contentRoots,
                                    @NotNull Collection<VirtualFile> appMapConfigFiles);
}
