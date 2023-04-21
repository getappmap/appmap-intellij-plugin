package appland.telemetry;

import appland.AppMapBaseTest;
import org.junit.Test;

public class IdentityTest extends AppMapBaseTest {
    @Test
    public void machineId() {
            assertNotNull("machine id must not be null or empty", Identity.getOrCreateMachineId());
    }
}