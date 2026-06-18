package appland.enterpriseConfig;

import appland.ProjectActivityAdapter;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Eagerly loads the organization configuration shortly after startup, on a background thread.
 * <p>
 * This is what lets the deployment-settings read path stay non-blocking: the live fetch happens
 * here (and on URL changes) instead of lazily on the first read of the settings. Running it once
 * per IDE session is enough; the fetch itself is idempotent.
 */
public class EnterpriseConfigStartupActivity extends ProjectActivityAdapter implements DumbAware {
    private static final AtomicBoolean STARTED = new AtomicBoolean(false);

    @Override
    public void runActivity(@NotNull Project project) {
        if (ApplicationManager.getApplication().isUnitTestMode()) {
            return;
        }

        if (STARTED.compareAndSet(false, true)) {
            // awaitInitialFetchIfConfigured blocks until the fetch completes, so run it off the
            // startup thread to avoid delaying other startup activities.
            ApplicationManager.getApplication().executeOnPooledThread(
                    EnterpriseConfigService::awaitInitialFetchIfConfigured);
        }
    }
}
