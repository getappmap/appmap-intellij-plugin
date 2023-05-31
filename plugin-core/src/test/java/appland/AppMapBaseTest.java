package appland;

import appland.cli.AppLandCommandLineService;
import appland.settings.AppMapApplicationSettings;
import appland.settings.AppMapApplicationSettingsService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixture4TestCase;
import org.jetbrains.annotations.NotNull;
import org.junit.After;

import java.util.Collections;

public abstract class AppMapBaseTest extends LightPlatformCodeInsightFixture4TestCase {
    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        // we're returning a new instance, because we don't want to share the project setup between light tests.
        // many of our tests require a clean filesystem.
        return new LightProjectDescriptor();
    }

    @After
    public void shutdownAppMapProcesses() {
        AppLandCommandLineService.getInstance().stopAll(true);

        // reset to default settings
        ApplicationManager.getApplication().getService(AppMapApplicationSettingsService.class).loadState(new AppMapApplicationSettings());
    }

    public @NotNull VirtualFile createAppMapWithIndexes(@NotNull String appMapName) throws Throwable {
        return createAppMapWithIndexes(appMapName, 0, 0, 0);
    }

    public @NotNull VirtualFile createAppMapWithIndexes(@NotNull String appMapName, int requestCount, int queryCount, int functionCount) throws Throwable {
        return WriteAction.computeAndWait((ThrowableComputable<VirtualFile, Throwable>) () -> {
            // create empty .appmap.json files because we're not indexing it
            var appMapFile = myFixture.createFile(appMapName + ".appmap.json", "");

            // create name/metadata.json with a name property
            var appMapDir = appMapFile.getParent().createChildDirectory(this, appMapName);
            VfsUtil.saveText(appMapDir.createChildData(this, "metadata.json"), "{\"name\": \"" + appMapName + "\"}");

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
