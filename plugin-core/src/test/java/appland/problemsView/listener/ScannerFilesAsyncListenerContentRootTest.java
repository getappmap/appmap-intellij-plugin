package appland.problemsView.listener;

import appland.AppMapBaseTest;
import appland.problemsView.FindingsManager;
import appland.problemsView.TestFindingsManager;
import com.intellij.testFramework.fixtures.TempDirTestFixture;
import com.intellij.testFramework.fixtures.impl.TempDirTestFixtureImpl;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

/**
 * Test of the async listener, which adds the folder with findings as content root to make sure it's found.
 */
public class ScannerFilesAsyncListenerContentRootTest extends AppMapBaseTest {
    @Override
    protected boolean runInDispatchThread() {
        return false;
    }

    @Override
    protected TempDirTestFixture createTempDirTestFixture() {
        // creates files on the local filesystem to enable the file watcher
        return new TempDirTestFixtureImpl();
    }

    @Test
    public void findingsFileWatcher() throws Exception {
        // create src/root to add it as content root to the current module
        // the file listener is using a project scope, which needs a properly set up content root
        var rootDir = myFixture.getTempDirFixture().findOrCreateDir("root");
        withContentRoot(rootDir, () -> {
            var condition = TestFindingsManager.createFindingsCondition(getProject(), getTestRootDisposable());
            // adding an appmap-findings.json file must trigger a refresh via the file watcher
            myFixture.copyDirectoryToProject("vscode/workspaces/project-system", "root");
            assertTrue("Findings must be reloaded when a appmap-findings.json is added", condition.await(60, TimeUnit.SECONDS));
        });

        assertEquals(1, FindingsManager.getInstance(getProject()).getProblemFileCount());
        assertEquals(1, FindingsManager.getInstance(getProject()).getProblemCount());
    }
}