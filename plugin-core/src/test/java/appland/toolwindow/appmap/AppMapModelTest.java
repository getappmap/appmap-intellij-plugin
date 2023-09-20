package appland.toolwindow.appmap;

import appland.AppMapBaseTest;
import com.intellij.openapi.application.WriteAction;
import com.intellij.testFramework.fixtures.TempDirTestFixture;
import com.intellij.testFramework.fixtures.impl.TempDirTestFixtureImpl;
import org.junit.Test;

import static appland.utils.ModelTestUtil.assertTreeHierarchy;

public class AppMapModelTest extends AppMapBaseTest {
    @Override
    protected boolean runInDispatchThread() {
        return false;
    }

    @Override
    protected TempDirTestFixture createTempDirTestFixture() {
        // create temp files on disk
        return new TempDirTestFixtureImpl();
    }

    @Test
    public void emptyAppMaps() {
        var model = new AppMapModel(getProject());
        var expected = "-AppMaps\n";
        assertTreeHierarchy(model, expected, getTestRootDisposable());
    }

    @Test
    public void hierarchyOneAppMapProject() throws Exception {
        // copy into parent dir, because the default location is "src/", which already is a content root
        var rootOne = WriteAction.computeAndWait(() -> myFixture.copyDirectoryToProject("projects/runtime_analysis_tree", "../root_one"));

        withContentRoot(getModule(), rootOne, () -> {
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

        withContentRoot(getModule(), rootOne, () -> {
            withContentRoot(getModule(), rootTwo, () -> {
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
        });
    }

    @Test
    public void emptyTree() {
        WriteAction.computeAndWait(() -> myFixture.copyDirectoryToProject("projects/empty-appMapDir", "root"));

        var model = new AppMapModel(getProject());
        var expected = "-AppMaps\n";
        assertTreeHierarchy(model, expected, getTestRootDisposable());
    }
}