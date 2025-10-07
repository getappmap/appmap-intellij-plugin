package appland.telemetry;

import appland.telemetry.appinsights.TelemetryEvent;
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread;
import org.jetbrains.annotations.NotNull;

public interface TelemetryReporter {
    @RequiresBackgroundThread
    void track(@NotNull TelemetryEvent event);
}
