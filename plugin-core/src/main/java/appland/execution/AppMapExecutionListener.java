package appland.execution;

import com.intellij.execution.ExecutionListener;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFileManager;
import org.jetbrains.annotations.NotNull;

/**
 * Force a sync of the filesystem after a AppMap run configuration terminated
 * to make sure that newly added AppMaps are found.
 */
public class AppMapExecutionListener implements ExecutionListener {
    private static final Logger LOG = Logger.getInstance(AppMapExecutionListener.class);

    @Override
    public void processTerminated(@NotNull String executorId, @NotNull ExecutionEnvironment env, @NotNull ProcessHandler handler, int exitCode) {
        if (executorId.equals(AppMapJvmExecutor.EXECUTOR_ID)) {
            VirtualFileManager.getInstance().asyncRefresh(() -> {
                LOG.debug("finished filesystem refresh after execution with AppMap");
            });
        }
    }
}
