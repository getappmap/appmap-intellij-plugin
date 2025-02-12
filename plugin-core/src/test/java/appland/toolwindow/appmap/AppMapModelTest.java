package appland.toolwindow.appmap;

import appland.AppMapBaseTest;
import appland.utils.ModuleTestUtils;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.testFramework.fixtures.TempDirTestFixture;
import com.intellij.testFramework.fixtures.impl.TempDirTestFixtureImpl;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.List;

import static appland.utils.ModelTestUtil.assertTreeHierarchy;

public class AppMapModelTest extends AppMapBaseTest {
    @Override
    protected TempDirTestFixture createTempDirTestFixture() {
        // create temp files on disk
        return new TempDirTestFixtureImpl();
    }

    @Override
    protected boolean runInDispatchThread() {
        return false;
}

    @Test
    public void emptyAppMaps() {
        var model = new AppMapModel(getProject());
        var expected = "-AppMaps\n";
        assertTreeHierarchy(model, expected, getTestRootDisposable());
    }

    @Test
    public void hierarchyWithRequestRecording() throws Exception {
        // copy into parent dir, because the default location is "src/", which already is a content root
        var rootOne = WriteAction.computeAndWait(() -> myFixture.copyDirectoryToProject("projects/appmap_request_sorting", "../root_one"));

        // first, copy file 1 then file 2
        copyTestDataAppMaps("projects/appmap_request_sorting", "/tmp/appmap/request_recording",
                "request_recording1.appmap.json",
                "request_recording2.appmap.json");

        withContentRoot(rootOne, () -> {
            var model = new AppMapModel(getProject());
            var expected = "-AppMaps\n" +
                    " -root_one\n" +
                    "  -Requests (java + request_recording)\n" +
                    "   GET /owners/1 (200) - 10:48:59.009\n" +
                    "   GET /oups (500) - 10:49:04.974\n";
            assertTreeHierarchy(model, expected, getTestRootDisposable());
        });

        // then, copy in reverse order to make file 1 the newest file and verify updated sort order
        copyTestDataAppMaps("projects/appmap_request_sorting", "/tmp/appmap/request_recording",
                "request_recording2.appmap.json",
                "request_recording1.appmap.json");

        withContentRoot(rootOne, () -> {
            var model = new AppMapModel(getProject());
            var expected = "-AppMaps\n" +
                    " -root_one\n" +
                    "  -Requests (java + request_recording)\n" +
                    "   GET /oups (500) - 10:49:04.974\n" +
                    "   GET /owners/1 (200) - 10:48:59.009\n";
            assertTreeHierarchy(model, expected, getTestRootDisposable());
        });
    }

    @Test
    public void hierarchyOneAppMapProject() throws Exception {
        // copy into parent dir, because the default location is "src/", which already is a content root
        var rootOne = WriteAction.computeAndWait(() -> myFixture.copyDirectoryToProject("projects/runtime_analysis_tree", "../root_one"));

        withContentRoot(rootOne, () -> {
            var model = new AppMapModel(getProject());
            var expected = "-AppMaps\n" +
                    " -root_one\n" +
                    "  -Tests (ruby + minitest)\n" +
                    "   Failed test 1\n" +
                    "   Failed test 2\n" +
                    "   Successful Test 1\n" +
                    "   Successful Test 2\n";
            assertTreeHierarchy(model, expected, getTestRootDisposable());
        });
    }

    @Test
    public void hierarchyTwoAppMapProjects() throws Exception {
        // copy into parent dir, because the default location is "src/", which already is a content root
        var rootOne = WriteAction.computeAndWait(() -> myFixture.copyDirectoryToProject("projects/runtime_analysis_tree", "../root_one"));
        var rootTwo = WriteAction.computeAndWait(() -> myFixture.copyDirectoryToProject("projects/runtime_analysis_tree", "../root_two"));

        withContentRoots(List.of(rootOne, rootTwo), () -> {
            var model = new AppMapModel(getProject());
            var expected = "-AppMaps\n" +
                    " -root_one\n" +
                    "  -Tests (ruby + minitest)\n" +
                    "   Failed test 1\n" +
                    "   Failed test 2\n" +
                    "   Successful Test 1\n" +
                    "   Successful Test 2\n" +
                    " -root_two\n" +
                    "  -Tests (ruby + minitest)\n" +
                    "   Failed test 1\n" +
                    "   Failed test 2\n" +
                    "   Successful Test 1\n" +
                    "   Successful Test 2\n";
            assertTreeHierarchy(model, expected, getTestRootDisposable());
        });
    }

    @Test
    public void emptyTree() {
        WriteAction.computeAndWait(() -> myFixture.copyDirectoryToProject("projects/empty-appMapDir", "root"));

        var model = new AppMapModel(getProject());
        var expected = "-AppMaps\n";
        assertTreeHierarchy(model, expected, getTestRootDisposable());
    }

    private void copyTestDataAppMaps(@NotNull String baseDirectoryPath,
                                     @NotNull String relativeFilePath,
                                     @NotNull String... filenames) throws InterruptedException {
        WriteAction.runAndWait(() -> {
            for (var filename : filenames) {
                var path = StringUtil.join(List.of(baseDirectoryPath, relativeFilePath, filename), "/");
                var targetPath = StringUtil.join(List.of("../root_one", relativeFilePath, filename), "/");
                myFixture.copyFileToProject(path, targetPath);
                // enforce a later modification date of the 2nd file
                Thread.sleep(500);
            }
        });
    }
}