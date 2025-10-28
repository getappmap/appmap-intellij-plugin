package appland.files;

import appland.cli.AppLandCommandLineService;
import appland.utils.GsonUtils;
import appland.webviews.WebviewEditorException;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.util.ExecUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixture4TestCase;
import com.intellij.testFramework.fixtures.TempDirTestFixture;
import com.intellij.testFramework.fixtures.impl.TempDirTestFixtureImpl;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class AppMapFilesTest extends LightPlatformCodeInsightFixture4TestCase {
    @Override
    protected TempDirTestFixture createTempDirTestFixture() {
        return new TempDirTestFixtureImpl();
    }

    @Override
    protected boolean runInDispatchThread() {
        return false;
    }

    @Test
    public void files() {
        var file = myFixture.configureByText("a.json", "");
        assertFalse(AppMapFiles.isAppMap(file.getVirtualFile()));

        file = myFixture.configureByText("a.appmap.json", "");
        assertTrue(AppMapFiles.isAppMap(file.getVirtualFile()));
    }

    @Test
    public void updateMapMapName() throws IOException {
        var file = FileUtilRt.createTempFile("appmap-test", ".appmap.json").toPath();
        Files.write(file, "{}".getBytes(StandardCharsets.UTF_8));

        var success = AppMapFiles.updateMetadata(file, "updated AppMap name", StandardCharsets.UTF_8);
        assertTrue(success);
        assertEquals("{\"metadata\":{\"name\":\"updated AppMap name\"}}", Files.readString(file));
    }

    @Test
    public void updateExistingMapMapName() throws IOException {
        var file = FileUtilRt.createTempFile("appmap-test", ".appmap.json").toPath();
        Files.write(file, "{\"metadata\": {\"name\":\"old AppMap name\"}}".getBytes(StandardCharsets.UTF_8));

        var success = AppMapFiles.updateMetadata(file, "updated AppMap name", StandardCharsets.UTF_8);
        assertTrue(success);
        assertEquals("{\"metadata\":{\"name\":\"updated AppMap name\"}}", Files.readString(file));
    }

    @Test
    public void readAppMapDir() {
        var configFile = myFixture.copyFileToProject("appmap-config/appmap.yml");
        assertEquals("tmp/appmap", AppMapFiles.readAppMapDirConfigValue(configFile));
    }

    @Test
    public void readAppMapDirEmptyValue() {
        var configFile = myFixture.copyFileToProject("appmap-config/appmap-empty-dir.yml");
        assertNull(AppMapFiles.readAppMapDirConfigValue(configFile));
    }

    @Test
    public void readAppMapDirMissingValue() {
        var configFile = myFixture.copyFileToProject("appmap-config/appmap-no-dir.yml");
        assertNull(AppMapFiles.readAppMapDirConfigValue(configFile));
    }

    @Test
    public void pruneLargeAppMapFile() throws WebviewEditorException {
        // file with a size of roughly 2.5mb
        var appMap = myFixture.copyFileToProject("appmap/misago_threads_tests_test_threadslists_AllThreadsListTests_test_noscript_pagination.appmap.json");
        var prunedContent = AppMapFiles.loadAppMapFile(appMap, 10 * 1024 * 1024, 2 * 1024 * 1024, "1mb");
        assertNotNull(prunedContent);

        var jsonPruned = GsonUtils.GSON.fromJson(prunedContent, JsonObject.class);
        assertNotNull(jsonPruned);
        assertTrue("Pruned data must contain property pruneFilter", jsonPruned.has("pruneFilter"));
    }

    @Test
    public void pruneGiantAppMapFile() throws WebviewEditorException {
        // file with a size of roughly 2.5mb
        var appMap = myFixture.copyFileToProject("appmap/misago_threads_tests_test_threadslists_AllThreadsListTests_test_noscript_pagination.appmap.json");
        var prunedContent = AppMapFiles.loadAppMapFile(appMap, 2 * 1024 * 1024, 2 * 1024 * 1024, "1mb");
        assertEquals("Empty JSON must be returned for a giant AppMap ", "{}", prunedContent);
    }

    @Test
    public void statsJson() throws ExecutionException {
        var appMap = myFixture.copyFileToProject("appmap/misago_threads_tests_test_threadslists_AllThreadsListTests_test_noscript_pagination.appmap.json");
        var command = AppLandCommandLineService.getInstance().createAppMapStatsCommand(appMap);
        assertNotNull(command);

        var statsArray = GsonUtils.GSON.fromJson(ExecUtil.execAndGetOutput(command).getStdout(), JsonArray.class);
        assertNotNull("Stats JSON must be valid, STDERR must be excluded from captured output", statsArray);

        assertTrue("stats must not be empty", statsArray.size() > 0);
    }
}