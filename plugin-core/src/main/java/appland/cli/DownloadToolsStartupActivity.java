package appland.cli;

import appland.ProjectActivityAdapter;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Executed in a background thread.
 * The download is only performed on the first opened project.
 */
public class DownloadToolsStartupActivity extends ProjectActivityAdapter implements DumbAware {
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
