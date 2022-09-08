package appland.problemsView;

import appland.AppMapBaseTest;
import appland.index.AppMapFindingsUtil;
import appland.problemsView.listener.ScannerFindingsListener;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class FindingsManagerTest extends AppMapBaseTest {
    @Before
    @After
    public void resetFindings() {
        TestFindingsManager.getInstance(getProject()).reset();

        var manager = FindingsManager.getInstance(getProject());
        assertEquals(0, manager.getProblemFileCount());
        assertEquals(0, manager.getProblemCount());
    }

    @Test
    public void fileNames() {
        assertTrue(AppMapFindingsUtil.isFindingFile("/parent/child/appmap-findings.json"));
        assertTrue(AppMapFindingsUtil.isFindingFile("appmap-findings.json"));

        assertFalse(AppMapFindingsUtil.isFindingFile("/parent/child/file.json"));
        assertFalse(AppMapFindingsUtil.isFindingFile("/parent/child/appmap-findings.yaml"));
    }

    @Test
    public void findingsVSCodeSystem() {
        var manager = FindingsManager.getInstance(getProject());
        var root = myFixture.copyDirectoryToProject("vscode/workspaces/project-system", "root");

        var findingsFile = root.findChild("appmap-findings.json");
        assertNotNull(findingsFile);

        var problematicFile = root.findFileByRelativePath("app/controllers/microposts_controller.rb");
        assertNotNull(problematicFile);

        manager.reload();

        assertEquals(1, manager.getProblemFileCount());
        assertEquals(1, manager.getProblemCount());
        assertEquals(List.of(problematicFile), manager.getProblemFiles());
        assertEquals(1, manager.getProblemCount(problematicFile));
        assertEquals(0, manager.getOtherProblemCount());
        assertEquals(0, manager.getOtherProblems().size());

        // removing the findings file must update the problems
        manager.removeFindingsFile(findingsFile.getPath());
        assertEquals(0, manager.getProblemFileCount());
        assertEquals(0, manager.getProblemCount());
        assertEquals(0, manager.getProblemFiles().size());
        assertEquals(0, manager.getProblemCount(problematicFile));
        assertEquals(0, manager.getOtherProblemCount());
        assertEquals(0, manager.getOtherProblems().size());
    }

    @Test
    public void findingsFileWatcher() throws InterruptedException {
        var manager = FindingsManager.getInstance(getProject());

        var condition = createAppMapFindingsCondition();
        // adding an appmap-findings.json file must trigger a refresh via the file watcher
        myFixture.copyDirectoryToProject("vscode/workspaces/project-system", "root");
        assertTrue(condition.await(10, TimeUnit.SECONDS));

        assertEquals(1, manager.getProblemFileCount());
        assertEquals(1, manager.getProblemCount());
    }

    private CountDownLatch createAppMapFindingsCondition() {
        var latch = new CountDownLatch(1);
        getProject().getMessageBus().connect(getTestRootDisposable()).subscribe(ScannerFindingsListener.TOPIC, latch::countDown);
        return latch;
    }
}