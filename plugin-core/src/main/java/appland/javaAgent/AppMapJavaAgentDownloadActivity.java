package appland.javaAgent;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;

public class AppMapJavaAgentDownloadActivity implements StartupActivity, DumbAware {
    private static final Logger LOG = Logger.getInstance(AppMapJavaAgentDownloadActivity.class);
    private static final AtomicBoolean ACTIVE = new AtomicBoolean(false);

    @Override
    public void runActivity(@NotNull Project project) {
        if (ApplicationManager.getApplication().isUnitTestMode()) {
            return;
        }

        if (ACTIVE.compareAndSet(false, true)) {
            try {
                LOG.debug("Downloading AppMap Java agent");
                AppMapJavaAgentDownloadService.getInstance().downloadJavaAgent(project);
            } catch (Exception e) {
                LOG.warn("Failed to download AppMap Java agent", e);
            }
        }
    }
}
