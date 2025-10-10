package appland.telemetry;

import org.junit.Test;

import static org.junit.Assert.assertFalse;

public class NoOpTelemetryReporterTest {
    @Test
    public void notForcefullyEnabled() {
        assertFalse(new NoOpTelemetryReporter().isAlwaysEnabled());
    }
}