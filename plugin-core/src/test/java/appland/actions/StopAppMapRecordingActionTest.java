package appland.actions;

import appland.AppMapBaseTest;
import appland.settings.AppMapProjectSettingsService;
import com.intellij.openapi.application.WriteAction;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Paths;

public class StopAppMapRecordingActionTest extends AppMapBaseTest {
    @Before
    @After
    public void resetSettings() {
        AppMapProjectSettingsService.getState(getProject()).setRecentAppMapStorageLocation("");
    }

    @Override
    protected boolean runInDispatchThread() {
        return false;
    }

    @Test
    public void findDefaultStorageLocationSettings() {
        AppMapProjectSettingsService.getState(getProject()).setRecentAppMapStorageLocation("/project/user/appMapDir");
        assertEquals(Paths.get("/project/user/appMapDir"), StopAppMapRecordingAction.findDefaultStorageLocation(getProject()));
    }

    @Test
    public void findDefaultStorageLocationConfig() {
        edt(() -> WriteAction.run(() -> myFixture.copyDirectoryToProject("projects/empty-appMapDir", "root")));

        var location = StopAppMapRecordingAction.findDefaultStorageLocation(getProject());
        assertNotNull(location);
        assertEquals("/src/root/tmp-appMapAgent/appmap/remote", location.toString());
    }

    @Test
    public void findDefaultStorageLocationFallback() {
        var location = StopAppMapRecordingAction.findDefaultStorageLocation(getProject());
        assertNotNull(location);
        assertEquals("/src/tmp/appmap/remote", location.toString());
    }
}