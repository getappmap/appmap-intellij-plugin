package appland.actions;

import appland.AppMapBaseTest;
import com.intellij.openapi.application.WriteAction;
import com.intellij.util.PathUtil;
import org.junit.Test;

public class StopAppMapRecordingActionTest extends AppMapBaseTest {
    @Override
    protected boolean runInDispatchThread() {
        return false;
    }

    @Test
    public void findDefaultStorageLocationConfig() {
        edt(() -> WriteAction.run(() -> myFixture.copyDirectoryToProject("projects/empty-appMapDir", "root")));

        var location = StopAppMapRecordingAction.findDefaultStorageLocation(getProject());
        assertNotNull(location);
        assertTrue(PathUtil.toSystemIndependentName(location.toString()).endsWith("/root/tmp-appMapAgent/appmap/remote"));
    }

    @Test
    public void findDefaultStorageLocationConfigTwoConfigurations() {
        edt(() -> WriteAction.run(() -> myFixture.copyDirectoryToProject("projects/empty-appMapDir", "root1")));
        edt(() -> WriteAction.run(() -> myFixture.copyDirectoryToProject("projects/empty-appMapDir", "root2")));

        var location = StopAppMapRecordingAction.findDefaultStorageLocation(getProject());
        assertNotNull(location);

        var actualPath = PathUtil.toSystemIndependentName(location.toString());
        assertTrue(actualPath.endsWith("/root1/tmp-appMapAgent/appmap/remote") || actualPath.endsWith("/root2/tmp-appMapAgent/appmap/remote"));
    }

    @Test
    public void findDefaultStorageLocationFallback() {
        var location = StopAppMapRecordingAction.findDefaultStorageLocation(getProject());
        assertNotNull(location);
        assertTrue(PathUtil.toSystemIndependentName(location.toString()).endsWith("/target/appmap/remote"));
    }
}