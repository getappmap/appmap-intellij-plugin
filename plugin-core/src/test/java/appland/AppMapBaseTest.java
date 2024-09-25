package appland;

import appland.cli.AppLandCommandLineService;
import appland.cli.TestCommandLineService;
import appland.config.AppMapConfigFile;
import appland.files.AppMapFiles;
import appland.problemsView.TestFindingsManager;
import appland.rpcService.AppLandJsonRpcListener;
import appland.rpcService.AppLandJsonRpcListenerAdapter;
import appland.rpcService.AppLandJsonRpcService;
import appland.settings.AppMapApplicationSettings;
import appland.settings.AppMapApplicationSettingsService;
import appland.utils.IndexTestUtils;
import appland.utils.ModuleTestUtils;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.ex.temp.TempFileSystem;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixture4TestCase;
import com.intellij.util.PathUtil;
import com.intellij.util.ThrowableRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public abstract class AppMapBaseTest extends LightPlatformCodeInsightFixture4TestCase {
    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        // we're returning a new instance, because we don't want to share the project setup between light tests.
        // many of our tests require a clean filesystem.
        return new LightProjectDescriptor();
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        if (ApplicationManager.getApplication().isDispatchThread()) {
            TempFileSystem.getInstance().cleanupForNextTest();
        } else {
            edt(() -> TempFileSystem.getInstance().cleanupForNextTest());
        }

        TestFindingsManager.reset(getProject());

        // init services to register its listeners
        AppLandJsonRpcService.getInstance(getProject());
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            try {
                resetState();
            } catch (Throwable e) {
                addSuppressedException(e);
            }

            try {
                shutdownAppMapProcesses();
            } catch (Throwable e) {
                addSuppressedException(e);
            }
        } finally {
            super.tearDown();
        }
    }

    public @NotNull VirtualFile createAppMapConfig(@NotNull VirtualFile parentDirectory, @NotNull Path appMapOutputPath) throws Exception {
        return WriteAction.<VirtualFile, Exception>computeAndWait(() -> {
            var file = parentDirectory.createChildData(this, AppMapFiles.APPMAP_YML);

            var configFile = new AppMapConfigFile();
            configFile.setAppMapDir(PathUtil.toSystemIndependentName(appMapOutputPath.toString()));
            configFile.setName("Test case config");
            configFile.writeTo(file);

            return file;
        });
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

    /**
     * Creates a minimal version of AppMap JSON, which contains the metadata with a name.
     */
    public String createAppMapMetadataJSON(@NotNull String name) {
        return String.format("{\"metadata\": { \"name\": \"%s\", \"source_location\": \"/src/%s.java\" }}", name, name);
    }

    public String createAppMapMetadataJSON(@NotNull String name, int requestCount, int queryCount, int functionCount) {
        var events = createAppMapEvents(requestCount, queryCount);
        var classMap = createClassMap(functionCount);
        return String.format("{\n\"metadata\": { \"name\": \"%s\" },\n \"events\": %s\n,\n \"classMap\": %s\n}", name, events, classMap);
    }

    @NotNull
    public VirtualFile createTempDir(@NotNull String name) {
        var psiFile = myFixture.addFileToProject(name + "/file.txt", "");
        return ReadAction.compute(() -> psiFile.getParent().getVirtualFile());
    }

    protected void withContentRoot(@NotNull VirtualFile contentRoot, @NotNull ThrowableRunnable<Exception> runnable) throws Exception {
        ModuleTestUtils.withContentRoot(getModule(), contentRoot, runnable);
    }

    protected void withContentRoots(@NotNull Collection<VirtualFile> contentRoots, @NotNull ThrowableRunnable<Exception> runnable) throws Exception {
        ModuleTestUtils.withContentRoots(getModule(), contentRoots, runnable);
    }

    protected void withExcludedFolder(@NotNull VirtualFile excludedFolder, @NotNull ThrowableRunnable<Exception> runnable) throws Exception {
        var hasContentRoot = Arrays.stream(ModuleRootManager.getInstance(getModule()).getContentRoots())
                .anyMatch(root -> VfsUtilCore.isAncestor(root, excludedFolder, false));
        assertTrue("Excluded folders must be located in a content root", hasContentRoot);

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

    protected void waitUntilIndexesAreReady() {
        IndexTestUtils.waitUntilIndexesAreReady(getProject());
    }

    protected void waitForJsonRpcServer() {
        var service = AppLandJsonRpcService.getInstance(getProject());
        if (service.isServerRunning()) {
            return;
        }

        var latch = createWaitForJsonRpcServerRestartCondition(true);
        ApplicationManager.getApplication().executeOnPooledThread(service::startServer);
        try {
            assertTrue("The AppMap JSON-RPC server must launch", latch.await(30, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            addSuppressedException(e);
        }
    }

    protected void waitForJsonRpcServerPort() {
        var service = AppLandJsonRpcService.getInstance(getProject());
        if (service.isServerRunning()) {
            return;
        }

        var latch = createWaitForJsonRpcServerPortCondition();
        ApplicationManager.getApplication().executeOnPooledThread(service::startServer);
        try {
            assertTrue("The AppMap JSON-RPC server must launch", latch.await(30, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            addSuppressedException(e);
        }
    }

    protected @NotNull CountDownLatch createWaitForJsonRpcServerRestartCondition(boolean allowStart) {
        var latch = new CountDownLatch(1);
        getProject().getMessageBus()
                .connect(getTestRootDisposable())
                .subscribe(AppLandJsonRpcListener.TOPIC, new AppLandJsonRpcListenerAdapter() {
                    @Override
                    public void serverStarted() {
                        if (allowStart) {
                            latch.countDown();
                        }
                    }

                    @Override
                    public void serverRestarted() {
                        latch.countDown();
                    }
                });
        return latch;
    }

    protected @NotNull CountDownLatch createWaitForJsonRpcServerPortCondition() {
        var latch = new CountDownLatch(1);
        getProject().getMessageBus()
                .connect(getTestRootDisposable())
                .subscribe(AppLandJsonRpcListener.TOPIC, new AppLandJsonRpcListenerAdapter() {
                    @Override
                    public void serverConfigurationUpdated(@NotNull Collection<VirtualFile> contentRoots,
                                                           @NotNull Collection<VirtualFile> appMapConfigFiles) {
                        if (AppLandJsonRpcService.getInstance(getProject()).getServerPort() != null) {
                            latch.countDown();
                        }
                    }
                });
        return latch;
    }

    private String createAppMapEvents(int requestCount, int queryCount) {
        var json = new StringBuilder();
        json.append("[");

        for (var i = 0; i < requestCount; i++) {
            json.append("{\"http_server_request\": {}}\n");
            if (i < requestCount - 1) {
                json.append(",");
            }
        }

        if (requestCount > 0 && queryCount > 0) {
            json.append(",");
        }

        for (var i = 0; i < queryCount; i++) {
            json.append("{\"sql_query\": {}}\n");
            if (i < queryCount - 1) {
                json.append(",");
            }
        }

        json.append("]");
        return json.toString();
    }

    private String createClassMap(int functionCount) {
        var json = new StringBuilder();
        json.append("[");

        for (var i = 0; i < functionCount; i++) {
            if (i % 2 == 0) {
                json.append("{\"type\": \"function\"}\n");
            } else {
                // nesting for odd numbers to test the recursive parsing
                json.append("{\"type\": \"package\", \"children\": [ {\"type\": \"function\"} ]}\n");
            }
            if (i < functionCount - 1) {
                json.append(",");
            }
        }

        json.append("]");
        return json.toString();
    }

    private void resetState() {
        TestCommandLineService.getInstance().reset();
    }

    private void shutdownAppMapProcesses() {
        try {
            AppLandCommandLineService.getInstance().stopAll(60_000, TimeUnit.MILLISECONDS);
            assertEmpty(AppLandCommandLineService.getInstance().getActiveRoots());
        } finally {
            // reset to default settings
            ApplicationManager.getApplication().getService(AppMapApplicationSettingsService.class).loadState(new AppMapApplicationSettings());

            var activeRoots = AppLandCommandLineService.getInstance().getActiveRoots();
            Assert.assertTrue("All AppMap CLIs must be terminated: " + activeRoots, activeRoots.isEmpty());
        }
    }
}
