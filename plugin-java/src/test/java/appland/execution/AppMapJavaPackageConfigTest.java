package appland.execution;

import appland.AppMapBaseTest;
import org.junit.Test;

public class AppMapJavaPackageConfigTest extends AppMapBaseTest {
    @Test
    public void systemIndependentAppMapDir() {
        var config = AppMapJavaPackageConfig.generateAppMapConfig(getModule(), "tmp\\appmap");
        assertEquals("tmp/appmap", config.getAppMapDir());
    }
}