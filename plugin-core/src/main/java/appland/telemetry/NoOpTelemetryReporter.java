package appland.telemetry;

import org.jetbrains.annotations.NotNull;

final class NoOpTelemetryReporter implements TelemetryReporter {
    @Override
    public void track(@NotNull TelemetryEvent event) {
        // no-op
    }
}
