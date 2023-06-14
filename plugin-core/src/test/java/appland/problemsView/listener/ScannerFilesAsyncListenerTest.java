package appland.problemsView.listener;

import appland.AppMapBaseTest;
import appland.problemsView.FindingsManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.testFramework.fixtures.TempDirTestFixture;
import com.intellij.testFramework.fixtures.impl.TempDirTestFixtureImpl;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static appland.problemsView.TestFindingsManager.createFindingsCondition;

public class ScannerFilesAsyncListenerTest extends AppMapBaseTest {
    @Override
    protected boolean runInDispatchThread() {
        return false;
    }

    @Override
    protected TempDirTestFixture createTempDirTestFixture() {
        // creates files on the local filesystem
        return new TempDirTestFixtureImpl();
    }

    @Test
    public void findingsFileWatcher() throws InterruptedException {
        var condition = createFindingsCondition(getProject(), getTestRootDisposable());
        // adding an appmap-findings.json file must trigger a refresh via the file watcher
        myFixture.copyDirectoryToProject("vscode/workspaces/project-system", "root");
        assertTrue(condition.await(60, TimeUnit.SECONDS));

        assertFindings(1, 1);
    }

    @Test
    public void findingsFileRename() throws InterruptedException {
        var file = myFixture.copyFileToProject("vscode/workspaces/project-system/appmap-findings.json", "a.json");
        var psiFile = ReadAction.compute(() -> PsiManager.getInstance(getProject()).findFile(file));

        var condition = createFindingsCondition(getProject(), getTestRootDisposable());
        WriteAction.runAndWait(() -> myFixture.renameElement(psiFile, "appmap-findings.json"));
        assertTrue("Renaming a file to appmap-findings.json must refresh", condition.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void findingsFileNameChange() throws InterruptedException {
        var file = copyFindingsFixtureFile();

        var condition = createFindingsCondition(getProject(), getTestRootDisposable());
        WriteAction.runAndWait(() -> myFixture.renameElement(file, "a.json"));
        assertTrue("Renaming a appmap-findings.json must refresh", condition.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void findingsFileDeletion() throws InterruptedException {
        var file = copyFindingsFixtureFile();

        var condition = createFindingsCondition(getProject(), getTestRootDisposable());
        WriteAction.runAndWait(file::delete);
        assertTrue("Removing an appmap-findings.json without findings must refresh", condition.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void findingsDirectoryDeletion() throws InterruptedException, IOException {
        var directory = copyFindingsFixtureDirectory();
        assertNotNull(directory);

        var condition = createFindingsCondition(getProject(), getTestRootDisposable());
        WriteAction.runAndWait(() -> directory.delete(this));
        assertTrue("Removing a directory with appmap-findings.json files must refresh", condition.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void emptyFindingsFileDeletion() throws InterruptedException {
        var file = myFixture.configureByText("appmap-findings.json", "{}");

        var condition = createFindingsCondition(getProject(), getTestRootDisposable());
        WriteAction.runAndWait(file::delete);
        assertFalse("Removing an appmap-findings.json without findings must not refresh", condition.await(10, TimeUnit.SECONDS));
    }

    private PsiFile copyFindingsFixtureFile() throws InterruptedException {
        var condition = createFindingsCondition(getProject(), getTestRootDisposable());
        var file = myFixture.copyFileToProject("vscode/workspaces/project-system/appmap-findings.json");
        assertTrue(condition.await(10, TimeUnit.SECONDS));

        return ReadAction.compute(() -> PsiManager.getInstance(getProject()).findFile(file));
    }

    private VirtualFile copyFindingsFixtureDirectory() throws InterruptedException {
        var condition = createFindingsCondition(getProject(), getTestRootDisposable());
        var dir = myFixture.copyDirectoryToProject("vscode/workspaces/project-system", "findings");
        assertTrue(condition.await(10, TimeUnit.SECONDS));
        return dir;
    }

    private void assertFindings(int problemFileCount, int problemCount) {
        assertEquals(problemFileCount, FindingsManager.getInstance(getProject()).getProblemFileCount());
        assertEquals(problemCount, FindingsManager.getInstance(getProject()).getProblemCount());
    }
}