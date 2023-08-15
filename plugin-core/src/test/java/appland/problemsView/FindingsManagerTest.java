package appland.problemsView;

import appland.AppMapBaseTest;
import appland.index.AppMapFindingsUtil;
import appland.problemsView.model.TestStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.Date;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
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
    public void findingsVSCodeSystem() throws Exception {
        var manager = FindingsManager.getInstance(getProject());

        var condition = TestFindingsManager.createFindingsCondition(getProject(), getTestRootDisposable());
        var root = myFixture.copyDirectoryToProject("vscode/workspaces/project-system", "root");
        assertTrue(condition.await(30, TimeUnit.SECONDS));

        var findingsFile = root.findChild("appmap-findings.json");
        assertNotNull(findingsFile);

        var problematicFile = root.findFileByRelativePath("app/controllers/microposts_controller.rb");
        assertNotNull(problematicFile);

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
    public void modificationDates() throws Exception {
        var manager = FindingsManager.getInstance(getProject());

        var condition = TestFindingsManager.createFindingsCondition(getProject(), getTestRootDisposable());
        var root = myFixture.copyDirectoryToProject("projects/with_modification_date", "root");
        assertTrue(condition.await(30, TimeUnit.SECONDS));

        var findingsFile = root.findFileByRelativePath("tmp/appmap/with_modification_date/appmap-findings.json");
        assertNotNull(findingsFile);

        var problematicFile = root.findFileByRelativePath("app/models/user.rb");
        assertNotNull(problematicFile);

        var findings = manager.getProblems(problematicFile);
        assertNotEmpty(findings);

        var problem = (ScannerProblem) findings.get(0);
        var metadata = problem.getFinding().getFindingsMetaData();
        assertNotNull(metadata);
        assertEquals(TestStatus.Succeeded, metadata.testStatus);

        // "eventsModifiedDate": "2022-04-12T14:25:33.000Z"
        var expectedDate = Date.from(Instant.from(DateTimeFormatter.ISO_INSTANT.parse("2022-04-12T14:25:33.000Z")));
        assertEquals(expectedDate, problem.getFinding().eventsModifiedDate);
    }
}