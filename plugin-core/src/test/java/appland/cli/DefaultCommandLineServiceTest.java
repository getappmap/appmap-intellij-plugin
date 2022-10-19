package appland.cli;

import appland.AppMapBaseTest;
import appland.settings.AppMapApplicationSettings;
import appland.settings.AppMapApplicationSettingsService;
import com.intellij.execution.ExecutionException;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.VfsTestUtil;
import com.intellij.testFramework.fixtures.TempDirTestFixture;
import com.intellij.testFramework.fixtures.impl.TempDirTestFixtureImpl;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class DefaultCommandLineServiceTest extends AppMapBaseTest {
    @Override
    protected TempDirTestFixture createTempDirTestFixture() {
        return new TempDirTestFixtureImpl();
    }

    @Override
    protected boolean runInDispatchThread() {
        return true;
    }

    @Before
    public void setupListener() {
        TestAppLandDownloadService.ensureDownloaded();

        RegisterContentRootsActivity.listenForContentRootChanges(getProject(), getTestRootDisposable());
        AppMapApplicationSettingsService.getInstance().setEnableFindings(true);
    }

    @After
    public void shutdownProcesses() {
        AppLandCommandLineService.getInstance().stopAll();
        // reset to default
        AppMapApplicationSettingsService.getInstance().setEnableFindings(new AppMapApplicationSettings().isEnableFindings());
    }

    @Test
    public void directoryTree() throws ExecutionException {
        var tempFile = myFixture.createFile("test.txt", "");
        var tempDir = tempFile.getParent();

        var nestedFile = myFixture.addFileToProject("parent/child/file.txt", "");
        var nestedDir = nestedFile.getParent().getVirtualFile();
        assertNotNull(nestedDir);

        var service = AppLandCommandLineService.getInstance();
        addContentRootAndLaunchService(nestedDir);
        assertEmptyRoots();

        createAppMapYaml(nestedDir);
        addContentRootAndLaunchService(nestedDir);
        assertActiveRoots(nestedDir);

        addContentRootAndLaunchService(tempDir);
        assertFalse(service.isRunning(tempDir, false));

        createAppMapYaml(tempDir);
        addContentRootAndLaunchService(tempDir);
        assertTrue(service.isRunning(tempDir, true));
        assertTrue(service.isRunning(tempDir, false));

        assertFalse(service.isRunning(nestedDir, true));
        assertTrue(service.isRunning(nestedDir, false));

        assertActiveRoots(tempDir);

        service.stop(tempDir);
        assertFalse(service.isRunning(tempDir, true));
        assertFalse(service.isRunning(tempDir, false));
        assertFalse(service.isRunning(nestedDir, true));
        assertFalse(service.isRunning(nestedDir, false));

        service.stopAll();
    }

    @Test
    public void siblingDirectories() throws ExecutionException {
        var dirA = myFixture.addFileToProject("parentA/file.txt", "").getParent().getVirtualFile();
        var dirB = myFixture.addFileToProject("parentB/file.txt", "").getParent().getVirtualFile();

        var service = AppLandCommandLineService.getInstance();

        // no appmap.yml files -> no processes
        addContentRootAndLaunchService(dirA);
        addContentRootAndLaunchService(dirB);
        assertEmptyRoots();

        // appmap.yml for dirA
        createAppMapYaml(dirA);
        addContentRootAndLaunchService(dirA);
        assertTrue(service.isRunning(dirA, true));
        assertFalse(service.isRunning(dirB, true));

        // appmap.yml for dirB and dirA
        createAppMapYaml(dirB);
        addContentRootAndLaunchService(dirB);
        assertTrue(service.isRunning(dirA, true));
        assertTrue(service.isRunning(dirB, true));

        assertActiveRoots(dirA, dirB);

        service.stopAll();
        assertFalse(service.isRunning(dirA, true));
        assertFalse(service.isRunning(dirB, true));
    }

    @Test
    public void contentRootUpdates() throws InterruptedException {
        var newRootA = myFixture.addFileToProject("parentA/file.txt", "").getParent().getVirtualFile();
        var nestedRootA = myFixture.addFileToProject("parentA/subDir/file.txt", "").getParent().getVirtualFile();
        var newRootB = myFixture.addFileToProject("parentB/file.txt", "").getParent().getVirtualFile();

        createAppMapYaml(newRootA);
        createAppMapYaml(newRootB);

        // add new roots and assert that the new processes are launched
        var condition = ProjectRefreshUtil.newProjectRefreshCondition(getTestRootDisposable());
        ModuleRootModificationUtil.updateModel(getModule(), model -> {
            model.addContentEntry(newRootA);
            model.addContentEntry(nestedRootA);
            model.addContentEntry(newRootB);
        });
        assertTrue(condition.await(30, TimeUnit.SECONDS));
        assertActiveRoots(newRootA, newRootB);

        AppLandCommandLineService.getInstance().stopAll();
    }

    @Test
    public void appmapYamlTrigger() throws InterruptedException, IOException {
        var newRootA = myFixture.addFileToProject("parentA/file.txt", "").getParent().getVirtualFile();

        var condition = ProjectRefreshUtil.newProjectRefreshCondition(getTestRootDisposable());
        ModuleRootModificationUtil.updateModel(getModule(), model -> {
            model.addContentEntry(newRootA);
        });
        assertTrue(condition.await(30, TimeUnit.SECONDS));
        // no watched roots because there's no appmap.yml
        assertEmptyRoots();

        // creating an appmap.yml file in a content root must trigger a refresh and the start of the CLI binaries
        condition = ProjectRefreshUtil.newProjectRefreshCondition(getTestRootDisposable());
        var appMapYaml = createAppMapYaml(newRootA);
        assertTrue(condition.await(30, TimeUnit.SECONDS));
        assertActiveRoots(newRootA);

        // removing the appmap.yml again must stop the service
        condition = ProjectRefreshUtil.newProjectRefreshCondition(getTestRootDisposable());
        WriteAction.runAndWait(() -> {
            appMapYaml.delete(this);
        });
        assertTrue(condition.await(30, TimeUnit.SECONDS));
        assertEmptyRoots();
    }

    private @NotNull VirtualFile createAppMapYaml(@NotNull VirtualFile directory) {
        return VfsTestUtil.createFile(directory, "appmap.yml", "");
    }

    private void assertEmptyRoots() {
        assertActiveRoots();
    }

    private void addContentRootAndLaunchService(@NotNull VirtualFile... contentRoots) throws ExecutionException {
        ModuleRootModificationUtil.updateModel(getModule(), model -> {
            for (var contentRoot : contentRoots) {
                model.addContentEntry(contentRoot);
            }
        });

        for (var contentRoot : contentRoots) {
            AppLandCommandLineService.getInstance().start(contentRoot);
        }
    }

    /**
     * Asserts that the detected AppLand content roots match the parameters
     */
    private void assertActiveRoots(@NotNull VirtualFile... roots) {
        assertEquals(Set.of(roots), Set.copyOf(AppLandCommandLineService.getInstance().getActiveRoots()));
    }
}