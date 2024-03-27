package appland.rpcService;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.Topic;

import java.util.Collection;

public interface AppLandJsonRpcListener {
    Topic<AppLandJsonRpcListener> TOPIC = Topic.create("appmap.jsonRpcListener", AppLandJsonRpcListener.class);

    void serverStarted();

    void serverStopped();

    void serverRestarted();

    void serverConfigurationUpdated(Collection<VirtualFile> appMapConfigFiles);
}
