package appland.settings;

import appland.cli.AppLandDownloadService;
import appland.javaAgent.AppMapJavaAgentDownloadService;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class AutoDownloadSettingsListener implements AppMapSettingsListener {
    private static final Logger LOG = Logger.getInstance(AutoDownloadSettingsListener.class);

    @NotNull private final Project project;

    public AutoDownloadSettingsListener(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public void autoUpdateToolsChanged() {
        if (DownloadSettings.isAssetDownloadEnabled()) {
            try {
                AppLandDownloadService.getInstance().queueDownloadTasks(project);
            } catch (IOException e) {
                LOG.warn("Failed to download AppMap CLI assets", e);
            }

            try {
                AppMapJavaAgentDownloadService.getInstance().downloadJavaAgent(project);
            } catch (Exception e) {
                LOG.warn("Failed to download AppMap Java agent", e);
            }
        }
    }
}
