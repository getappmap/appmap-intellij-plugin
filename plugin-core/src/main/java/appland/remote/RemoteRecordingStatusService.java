package appland.remote;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Project service which keeps track of an active recording within the current project.
 */
public interface RemoteRecordingStatusService {
    @NotNull
    static RemoteRecordingStatusService getInstance(@NotNull Project project) {
        return project.getService(RemoteRecordingStatusService.class);
    }

    void recordingStarted(@NotNull String baseURL);

    void recordingStopped(@NotNull String baseURL);

    @Nullable
    String getActiveRecordingURL();
}
