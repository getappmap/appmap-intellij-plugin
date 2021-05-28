package appland.remote;

import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/**
 * Service to manage AppMap remote recordings.
 * <p>
 * The methods work synchronously, i.e. the caller has to make sure that the methods are not called on the EDT.
 */
public interface RemoteRecordingService {
    @NotNull
    static RemoteRecordingService getInstance() {
        return ApplicationManager.getApplication().getService(RemoteRecordingService.class);
    }

    /**
     * Retrieve the current status of remote recording
     *
     * @param baseURL The URL where the AppMap remote agent is installed, e.g. http://host.name
     * @return {@code true} if recording is active, {@code false} if it's not.
     */
    boolean isRecording(@NotNull String baseURL);

    /**
     * Starts AppMap remote recording
     *
     * @param baseURL The URL where the AppMap remote agent is installed.
     */
    boolean startRecording(@NotNull String baseURL);

    /**
     * Stops AppMap remote recording
     *
     * @param baseURL The URL where the AppMap remote agent is installed.
     */
    boolean stopRecording(@NotNull String baseURL, @NotNull Path targetFilePath);
}
