package appland.problemsView.listener;

import appland.AppMapLocalTempFilesTest;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static appland.problemsView.TestFindingsManager.createFindingsCondition;

public class ScannerFilesAsyncListenerTest extends AppMapLocalTempFilesTest {
    @Override
    protected boolean runInDispatchThread() {
        return false;
    }

    @Test
    public void findingsFileRename() throws InterruptedException {
        // target name must be different from appmap-findings.json to make the rename succeed
        var file = WriteAction.computeAndWait(() -> myFixture.copyFileToProject("vscode/workspaces/project-system/appmap-findings.json", "a.json"));
        var psiFile = ReadAction.compute(() -> PsiManager.getInstance(getProject()).findFile(file));

        var condition = createFindingsCondition(getProject(), getTestRootDisposable());
        WriteAction.runAndWait(() -> myFixture.renameElement(psiFile, "appmap-findings.json"));
        assertTrue("Renaming a file to appmap-findings.json must refresh", condition.await(30, TimeUnit.SECONDS));
    }

    @Test
    public void findingsFileNameChange() throws InterruptedException {
        var file = copyFindingsFixtureFile();

        var condition = createFindingsCondition(getProject(), getTestRootDisposable());
        WriteAction.runAndWait(() -> myFixture.renameElement(file, "a.json"));
        assertTrue("Renaming a appmap-findings.json must refresh", condition.await(30, TimeUnit.SECONDS));
    }

    @Test
    public void findingsFileDeletion() throws InterruptedException {
        var file = copyFindingsFixtureFile();

        var condition = createFindingsCondition(getProject(), getTestRootDisposable());
        WriteAction.runAndWait(file::delete);
        assertTrue("Removing an appmap-findings.json without findings must refresh", condition.await(30, TimeUnit.SECONDS));
    }

    @Test
    public void findingsDirectoryDeletion() throws InterruptedException, IOException {
        var directory = copyFindingsFixtureDirectory();
        assertNotNull(directory);

        var condition = createFindingsCondition(getProject(), getTestRootDisposable());
        WriteAction.runAndWait(() -> directory.delete(this));
        assertTrue("Removing a directory with appmap-findings.json files must refresh", condition.await(30, TimeUnit.SECONDS));
    }

    @Test
    public void emptyFindingsFileDeletion() throws InterruptedException {
        var file = myFixture.configureByText("appmap-findings.json", "{}");

        var condition = createFindingsCondition(getProject(), getTestRootDisposable(), true, false);
        WriteAction.runAndWait(file::delete);
        assertFalse("Removing an appmap-findings.json without findings must not refresh", condition.await(10, TimeUnit.SECONDS));
    }

    private PsiFile copyFindingsFixtureFile() throws InterruptedException {
        var condition = createFindingsCondition(getProject(), getTestRootDisposable(), false, true);
        var file = WriteAction.computeAndWait(() -> myFixture.copyFileToProject("vscode/workspaces/project-system/appmap-findings.json"));
        assertTrue(condition.await(30, TimeUnit.SECONDS));

        return ReadAction.compute(() -> PsiManager.getInstance(getProject()).findFile(file));
    }

    private VirtualFile copyFindingsFixtureDirectory() throws InterruptedException {
        var condition = createFindingsCondition(getProject(), getTestRootDisposable(), false, true);
        var dir = WriteAction.computeAndWait(() -> myFixture.copyDirectoryToProject("vscode/workspaces/project-system", "findings"));
        assertTrue(condition.await(30, TimeUnit.SECONDS));
        return dir;
    }
}