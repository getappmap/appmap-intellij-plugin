package appland.rpcService;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Adapter with empty default implementations.
 */
public abstract class AppLandJsonRpcListenerAdapter implements AppLandJsonRpcListener {
    @Override
    public void serverStarted() {
    }

    @Override
    public void serverStopped() {
    }

    @Override
    public void serverRestarted() {
    }

    @Override
    public void serverConfigurationUpdated(@NotNull Collection<VirtualFile> contentRoots,
                                           @NotNull Collection<VirtualFile> appMapConfigFiles) {
    }
}
