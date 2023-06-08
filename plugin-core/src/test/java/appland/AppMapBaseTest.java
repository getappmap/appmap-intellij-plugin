package appland;

import appland.cli.AppLandCommandLineService;
import appland.settings.AppMapApplicationSettings;
import appland.settings.AppMapApplicationSettingsService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixture4TestCase;
import com.intellij.util.ThrowableRunnable;
import org.jetbrains.annotations.NotNull;
import org.junit.After;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

public abstract class AppMapBaseTest extends LightPlatformCodeInsightFixture4TestCase {
    @After
    public void shutdownAppMapProcesses() {
        AppLandCommandLineService.getInstance().stopAll(true);

        // reset to default settings
        ApplicationManager.getApplication().getService(AppMapApplicationSettingsService.class).loadState(new AppMapApplicationSettings());
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

    protected void withContentRoot(@NotNull VirtualFile contentRoot, @NotNull ThrowableRunnable<Exception> runnable) throws Exception {
        assertTrue(contentRoot.isDirectory());

        var contentEntryRef = new AtomicReference<ContentEntry>();
        try {
            // hack to use ModuleRootModificationUtil and to keep a reference to the new entry
            ModuleRootModificationUtil.updateModel(getModule(), modifiableRootModel -> {
                contentEntryRef.set(modifiableRootModel.addContentEntry(contentRoot));
            });

            runnable.run();
        } finally {
            // remove again to avoid breaking follow-up tests
            var newEntry = contentEntryRef.get();
            assertNotNull(newEntry);

            ModuleRootModificationUtil.updateModel(getModule(), modifiableRootModel -> {
                modifiableRootModel.removeContentEntry(newEntry);
            });
        }
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
}
