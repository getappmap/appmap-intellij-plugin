package appland.utils;

import com.intellij.execution.process.KillableProcessHandler;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public final class AppMapProcessUtil {
    private static final Logger LOG = Logger.getInstance(AppMapProcessUtil.class);

    private AppMapProcessUtil() {
    }

    public static void terminateProcess(@NotNull KillableProcessHandler process, int timeout, @NotNull TimeUnit timeUnit) {
        LOG.debug("Terminating process: " + process.getCommandLine() + " timeout " + timeUnit.toMillis(timeout) + "ms");

        // AppMap processes don't seem to like a graceful shutdown
        process.setShouldKillProcessSoftly(false);

        if (ApplicationManager.getApplication().isUnitTestMode()) {
            // synchronously kills the process in test mode
            process.killProcess();
        } else {
            process.destroyProcess();
            if (!process.waitFor(500)) {
                LOG.warn("Process did not terminate within 500ms: " + process.getCommandLine());
            }

            if (!process.isProcessTerminated()) {
                process.killProcess();
            }
        }

        waitForProcess(process, timeout, timeUnit);
    }

    public static void waitForProcess(@NotNull KillableProcessHandler process, int timeout, @NotNull TimeUnit timeUnit) {
        if (timeout <= 0 || process.isProcessTerminated()) {
            return;
        }

        var deadline = System.currentTimeMillis() + timeUnit.toMillis(timeout);
        while (System.currentTimeMillis() < deadline) {
            if (process.isProcessTerminated()) {
                break;
            }

            process.waitFor(100);
        }
    }
}
