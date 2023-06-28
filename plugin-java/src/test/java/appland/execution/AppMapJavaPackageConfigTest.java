package appland.execution;

import appland.AppMapBaseTest;
import appland.index.AppMapSearchScopes;
import org.junit.Test;

public class AppMapJavaPackageConfigTest extends AppMapBaseTest {
    @Test
    public void systemIndependentAppMapDir() {
        var scope = AppMapSearchScopes.projectFilesWithExcluded(getProject());
        var config = AppMapJavaPackageConfig.generateAppMapConfig(getProject(), scope, "tmp\\appmap");
        assertEquals("tmp/appmap", config.getAppMapDir());
    }
}