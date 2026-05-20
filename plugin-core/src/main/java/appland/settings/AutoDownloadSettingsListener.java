package appland.settings;

import appland.cli.AppLandDownloadService;
import appland.cli.ManifestManager;
import appland.javaAgent.AppMapJavaAgentDownloadService;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class AutoDownloadSettingsListener implements AppMapSettingsListener {
    private static final Logger LOG = Logger.getInstance(AutoDownloadSettingsListener.class);

    @NotNull private final Project project;

    public AutoDownloadSettingsListener(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public void autoUpdateToolsChanged() {
        if (DownloadSettings.isAssetDownloadEnabled()) {
            ManifestManager.clearCache();
            AppLandDownloadService.getInstance().queueDownloadTasks(project);

            try {
                AppMapJavaAgentDownloadService.getInstance().downloadJavaAgent(project);
            } catch (Exception e) {
                LOG.warn("Failed to download AppMap Java agent", e);
            }
        }
    }
}
