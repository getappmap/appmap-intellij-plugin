package appland.problemsView.listener;

import appland.AppMapLocalTempFilesTest;
import appland.problemsView.FindingsManager;
import appland.problemsView.TestFindingsManager;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

/**
 * Test of the async listener, which adds the folder with findings as content root to make sure it's found.
 */
public class ScannerFilesAsyncListenerContentRootTest extends AppMapLocalTempFilesTest {
    @Override
    protected boolean runInDispatchThread() {
        return false;
    }

    @Test
    public void findingsFileWatcher() throws Exception {
        var condition = TestFindingsManager.createFindingsCondition(getProject(), getTestRootDisposable());

        // adding an appmap-findings.json file must trigger a refresh via the file watcher
        myFixture.copyDirectoryToProject("vscode/workspaces/project-system", "root");
        assertTrue("Findings must be reloaded when a appmap-findings.json is added", condition.await(60, TimeUnit.SECONDS));

        assertEquals(1, FindingsManager.getInstance(getProject()).getProblemFileCount());
        assertEquals(1, FindingsManager.getInstance(getProject()).getProblemCount());
    }
}