package appland.remote;

import appland.settings.AppMapProjectSettingsService;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DefaultRemoteRecordingStatusService implements RemoteRecordingStatusService {
    private static final Logger LOG = Logger.getInstance("#appland.remote");

    @NotNull
    private final Project project;

    public DefaultRemoteRecordingStatusService(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public void recordingStarted(@NotNull String baseURL) {
        LOG.debug("recording started: " + baseURL);
        AppMapProjectSettingsService.getState(project).setActiveRecordingURL(baseURL);
    }

    @Override
    public void recordingStopped(@NotNull String baseURL) {
        LOG.debug("recording stopped: " + baseURL);
        AppMapProjectSettingsService.getState(project).setActiveRecordingURL(null);
    }

    @Override
    public @Nullable String getActiveRecordingURL() {
        return AppMapProjectSettingsService.getState(project).getActiveRecordingURL();
    }
}
