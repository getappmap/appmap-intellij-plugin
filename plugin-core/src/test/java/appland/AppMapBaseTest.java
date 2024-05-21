package appland;

import appland.cli.AppLandCommandLineListener;
import appland.cli.AppLandCommandLineService;
import appland.cli.TestCommandLineService;
import appland.files.AppMapFiles;
import appland.problemsView.TestFindingsManager;
import appland.rpcService.AppLandJsonRpcService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.EdtTestUtil;
import com.intellij.testFramework.HeavyPlatformTestCase;
import com.intellij.testFramework.VfsTestUtil;
import com.intellij.testFramework.builders.EmptyModuleFixtureBuilder;
import com.intellij.testFramework.fixtures.CodeInsightFixtureTestCase;
import com.intellij.testFramework.fixtures.ModuleFixture;
import com.intellij.util.ThrowableRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RunWith(JUnit4.class)
public abstract class AppMapBaseTest extends CodeInsightFixtureTestCase<EmptyModuleFixtureBuilder<ModuleFixture>> {
    @Before
    public void setupAppMapTest() {
        TestFindingsManager.reset(getProject());

        // init services to register its listeners
        AppLandJsonRpcService.getInstance(getProject());
    }

    @Override
    protected void tearDown() throws Exception {
        // Even though the processes are stopped when the service is disposed,
        // the timeout in dispose() may not be enough to prevent the "leaked thread" detection.
        runSafe(() -> AppLandCommandLineService.getInstance().stopAll(60, TimeUnit.SECONDS));

        runSafe(() -> AppLandJsonRpcService.getInstance(getProject()).stopServerSync(60, TimeUnit.SECONDS));

        // runSafe(AppMapBaseTest::waitForAppMapThreadTermination);

        runSafe(this::resetState);

        runSafe(() -> {
            if (ApplicationManager.getApplication() instanceof ApplicationEx) {
                EdtTestUtil.runInEdtAndWait(() -> HeavyPlatformTestCase.cleanupApplicationCaches(getProject()));
            }
        });

        EdtTestUtil.runInEdtAndWait(super::tearDown);
    }

    public @NotNull Module getModule() {
        return myModule;
    }

    public @NotNull VirtualFile createAppMapWithIndexes(@NotNull String appMapName) throws Throwable {
        return createAppMapWithIndexes(appMapName, 0, 0, 0, appMapName);
    }

    public @NotNull VirtualFile createAppMapWithIndexes(@NotNull String appMapName,
                                                        int requestCount,
                                                        int queryCount,
                                                        int functionCount,
                                                        @Nullable String appMapNameProperty) throws Throwable {
        var namePropertyValue = appMapNameProperty != null ? "\"" + appMapNameProperty + "\"" : null;

        return WriteAction.computeAndWait((ThrowableComputable<VirtualFile, Throwable>) () -> {
            // create empty .appmap.json files because we're not indexing it
            var appMapFile = myFixture.createFile(appMapName + ".appmap.json", "");

            // create name/metadata.json with a name property
            var appMapDir = appMapFile.getParent().createChildDirectory(this, appMapName);
            VfsUtil.saveText(appMapDir.createChildData(this, "metadata.json"), "{\"name\": " + namePropertyValue + "}");

            if (requestCount > 0) {
                var json = "[" + StringUtil.trimEnd(StringUtil.repeat("{},", requestCount), ",") + "]";
                VfsUtil.saveText(appMapDir.createChildData(this, "canonical.httpServerRequests.json"), json);
            }

            if (queryCount > 0) {
                var json = "[" + StringUtil.trimEnd(StringUtil.repeat("{},", queryCount), ",") + "]";
                VfsUtil.saveText(appMapDir.createChildData(this, "canonical.sqlNormalized.json"), json);
            }

            if (functionCount > 0) {
                var json = new StringBuilder("[");
                for (var i = 0; i < functionCount; i++) {
                    json.append("{\"name\": \"function_").append(i).append("\", \"type\": \"function\"}");
                    if (i < functionCount - 1) {
                        json.append(",");
                    }
                }
                json.append("]");
                VfsUtil.saveText(appMapDir.createChildData(this, "classMap.json"), json.toString());
            }

            return appMapFile;
        });
    }

    @NotNull
    public VirtualFile createTempDir(@NotNull String name) {
        var psiFile = myFixture.addFileToProject(name + "/file.txt", "");
        return ReadAction.compute(() -> psiFile.getParent().getVirtualFile());
    }

    protected void withExcludedFolder(@NotNull VirtualFile excludedFolder, @NotNull Runnable runnable) {
        try {
            ModuleRootModificationUtil.updateExcludedFolders(getModule(), excludedFolder.getParent(),
                    Collections.emptyList(),
                    Collections.singletonList(excludedFolder.getUrl()));

            runnable.run();
        } finally {
            // un-exclude again to avoid follow-up tests
            ModuleRootModificationUtil.updateExcludedFolders(getModule(), excludedFolder.getParent(),
                    Collections.singletonList(excludedFolder.getUrl()),
                    Collections.emptyList());
        }
    }

    protected @NotNull VirtualFile createAppMapYaml(@NotNull VirtualFile directory) throws InterruptedException {
        return createAppMapYaml(directory, null);
    }

    protected @NotNull VirtualFile createAppMapYaml(@NotNull VirtualFile directory, @Nullable String appMapPath) throws InterruptedException {
        // a change to an appmap.yml is only applied if it's located in a content root
        assertNotNull("appmap.yml must be located in a content root", ReadAction.compute(() -> {
            return AppMapFiles.findTopLevelContentRoot(getProject(), directory);
        }));

        var refreshLatch = new CountDownLatch(1);
        var bus = ApplicationManager.getApplication().getMessageBus().connect(getTestRootDisposable());
        bus.subscribe(AppLandCommandLineListener.TOPIC, refreshLatch::countDown);
        try {
            var content = appMapPath != null ? "appmap_dir: " + appMapPath + "\n" : "";
            return VfsTestUtil.createFile(directory, "appmap.yml", content);
        } finally {
            // Creating a new appmap.yml file triggers the start of the CLI processes,
            // we have to wait for them to before executing the following tests.
            assertTrue("The AppLand services must launch when a new appmap.yaml is created", refreshLatch.await(30, TimeUnit.SECONDS));
        }
    }

    private void resetState() {
        TestCommandLineService.getInstance().reset();
    }

    private void runSafe(@NotNull ThrowableRunnable<Exception> runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            addSuppressedException(e);
        }
    }

    private static void waitForAppMapThreadTermination() throws Exception {
        var deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(30);
        while (System.currentTimeMillis() < deadline) {
            var appMapThreads = findAliveAppMapThreads();
            if (appMapThreads.isEmpty()) {
                return;
            }

            for (var thread : appMapThreads) {
                LOG.debug("Waiting for AppMap thread to terminate: " + thread.getName());
                thread.join(500);
            }
        }

        var appMapThreads = findAliveAppMapThreads();
        if (!appMapThreads.isEmpty()) {
            throw new RuntimeException("Leaked AppMap threads: " + appMapThreads);
        }
    }

    private static List<Thread> findAliveAppMapThreads() {
        return Thread.getAllStackTraces().keySet()
                .stream()
                .filter(thread -> thread.getName().toLowerCase().contains("appmap") && thread.isAlive())
                .collect(Collectors.toList());
    }
}
