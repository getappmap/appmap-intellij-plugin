package appland.cli;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Executed in a background thread.
 * The download is only performed on the first opened project.
 */
public class DownloadToolsStartupActivity implements StartupActivity.Background {
    private static final Logger LOG = Logger.getInstance(DownloadToolsStartupActivity.class);
    private static final AtomicBoolean ACTIVE = new AtomicBoolean(false);

    @Override
    public void runActivity(@NotNull Project project) {
        if (ApplicationManager.getApplication().isUnitTestMode()) {
            return;
        }

        if (ACTIVE.compareAndSet(false, true)) {
            try {
                AppLandDownloadService.getInstance().queueDownloadTasks(project);
            } catch (IOException e) {
                LOG.warn("Download of CLI binaries failed", e);
            }
        }
    }
}
