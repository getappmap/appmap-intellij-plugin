package appland.cli;

import appland.AppMapBaseTest;
import appland.settings.AppMapApplicationSettingsService;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.PtyCommandLine;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.VfsTestUtil;
import com.intellij.testFramework.fixtures.TempDirTestFixture;
import com.intellij.testFramework.fixtures.impl.TempDirTestFixtureImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
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
        AppMapApplicationSettingsService.getInstance().setApiKey("api-key");
    }

    @Test
    public void directoryTree() throws Exception {
        var tempFile = myFixture.createFile("test.txt", "");
        var tempDir = tempFile.getParent();

        var nestedFile = myFixture.addFileToProject("parent/child/file.txt", "");
        assertNotNull(nestedFile.getParent());

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

        service.stop(tempDir, true);
        assertFalse(service.isRunning(tempDir, true));
        assertFalse(service.isRunning(tempDir, false));
        assertFalse(service.isRunning(nestedDir, true));
        assertFalse(service.isRunning(nestedDir, false));
    }

    @Test
    public void directoryTreeWatchedSubdir() throws Exception {
        var tempDir = myFixture.createFile("test.txt", "").getParent();
        createAppMapYaml(tempDir);

        var service = AppLandCommandLineService.getInstance();
        createAppMapYaml(tempDir, "tmp/appmaps");
        addContentRootAndLaunchService(tempDir);
        assertActiveRoots(tempDir);

        assertTrue(service.isRunning(tempDir, false));
        var tempDirNioPath = LocalFileSystem.getInstance().getNioPath(tempDir);
        assertNotNull(tempDirNioPath);
        var appMapDirNioPath = tempDirNioPath.resolve("tmp/appmaps");
        assertTrue("Configured AppMap dir must be created: " + appMapDirNioPath, Files.isDirectory(appMapDirNioPath));
    }

    @Test
    public void siblingDirectories() throws Exception {
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

        service.stopAll(true);
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
    }

    @Test
    public void installCommandLineHasPty() {
        var installCommand = AppLandCommandLineService.getInstance().createInstallCommand(Paths.get("/"), "java");
        assertTrue("The install command must have PTY", installCommand instanceof PtyCommandLine);
    }

    @Test
    @Ignore("unstable test")
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

    private @NotNull VirtualFile createAppMapYaml(@NotNull VirtualFile directory) throws InterruptedException {
        return createAppMapYaml(directory, null);
    }

    private @NotNull VirtualFile createAppMapYaml(@NotNull VirtualFile directory, @Nullable String appMapPath) throws InterruptedException {
        var refreshLatch = new CountDownLatch(1);
        var bus = ApplicationManager.getApplication().getMessageBus().connect(getTestRootDisposable());
        bus.subscribe(AppLandCommandLineListener.TOPIC, new AppLandCommandLineListener() {
            @Override
            public void afterRefreshForProjects() {
                refreshLatch.countDown();
            }
        });

        try {
            var content = appMapPath != null ? "appmap_dir: " + appMapPath + "\n" : "";
            return VfsTestUtil.createFile(directory, "appmap.yml", content);
        } finally {
            // creating a new appmap.yml file triggers the start of the CLI processes,
            // we have to wait for them to avoid launch in the background to interact with the further tests
            assertTrue(refreshLatch.await(10, TimeUnit.SECONDS));
        }
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
            AppLandCommandLineService.getInstance().start(contentRoot, true);
        }
    }

    /**
     * Asserts that the detected AppLand content roots match the parameters
     */
    private void assertActiveRoots(@NotNull VirtualFile... roots) {
        var expectedRoots = Set.of(roots);
        var expectedString = StringUtil.join(expectedRoots, VirtualFile::toString, ", ");

        var activeRoots = Set.copyOf(AppLandCommandLineService.getInstance().getActiveRoots());
        var activeRootsString = StringUtil.join(activeRoots, VirtualFile::toString, ", ");

        assertEquals("Roots don't match. Expected: " + expectedString + ", actual: " + activeRootsString, expectedRoots, activeRoots);
    }
}