package appland.actions;

import appland.AppMapBaseTest;
import com.intellij.openapi.application.WriteAction;
import com.intellij.util.PathUtil;
import org.junit.Test;

import java.util.Set;

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
        assertEquals("/src/root/tmp-appMapAgent/appmap/remote", PathUtil.toSystemIndependentName(location.toString()));
    }

    @Test
    public void findDefaultStorageLocationConfigTwoConfigurations() {
        edt(() -> WriteAction.run(() -> myFixture.copyDirectoryToProject("projects/empty-appMapDir", "root1")));
        edt(() -> WriteAction.run(() -> myFixture.copyDirectoryToProject("projects/empty-appMapDir", "root2")));

        var location = StopAppMapRecordingAction.findDefaultStorageLocation(getProject());
        assertNotNull(location);
        var possibleResults = Set.of("/src/root1/tmp-appMapAgent/appmap/remote", "/src/root2/tmp-appMapAgent/appmap/remote");
        assertTrue(possibleResults.contains(PathUtil.toSystemIndependentName(location.toString())));
    }

    @Test
    public void findDefaultStorageLocationFallback() {
        var location = StopAppMapRecordingAction.findDefaultStorageLocation(getProject());
        assertNotNull(location);
        assertEquals("/src/target/appmap/remote", PathUtil.toSystemIndependentName(location.toString()));
    }
}