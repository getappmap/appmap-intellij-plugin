package appland.telemetry;

import com.intellij.util.concurrency.annotations.RequiresBackgroundThread;
import org.jetbrains.annotations.NotNull;

/**
 * Interface responsible for sending telemetry events to a server.
 */
public interface TelemetryReporter {
    /**
     * Send a telemetry event to the server.
     * This method must be invoked on a background thread.
     *
     * @param event The telemetry event to send.
     */
    @RequiresBackgroundThread
    void track(@NotNull TelemetryEvent event);

    /**
     * @return {@code true} if the reporter is forcefully enabled and the user settings to turn off telemetry must not be ignored..
     */
    default boolean isAlwaysEnabled() {
        return false;
    }
}
