package appland.toolwindow.runtimeAnalysis;

import appland.AppMapBaseTest;
import appland.problemsView.TestFindingsManager;
import appland.settings.AppMapApplicationSettingsService;
import appland.toolwindow.runtimeAnalysis.nodes.FindingsTableNode;
import appland.toolwindow.runtimeAnalysis.nodes.Node;
import appland.toolwindow.runtimeAnalysis.nodes.RootNode;
import com.intellij.openapi.application.WriteAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static appland.utils.ModelTestUtil.assertTreeHierarchy;

public class RuntimeAnalysisModelTest extends AppMapBaseTest {
    @Override
    protected boolean runInDispatchThread() {
        return false;
    }

    @Test
    public void unauthenticated() {
        AppMapApplicationSettingsService.getInstance().setApiKey(null);

        var model = new RuntimeAnalysisModel(getProject(), getTestRootDisposable());
        assertNotNull("The model must not track signed-in state", model.getRoot());
    }

    @Test
    public void authenticatedWithoutFindings() {
        AppMapApplicationSettingsService.getInstance().setApiKey("dummy");
        try {
            var model = new RuntimeAnalysisModel(getProject(), getTestRootDisposable());
            assertNotNull("Empty panel must NOT be shown if the user is signed in", model.getRoot());
            assertOverviewNode(model.getRoot());
        } finally {
            AppMapApplicationSettingsService.getInstance().setApiKey(null);
        }
    }

    @Test
    public void authenticatedWithFindings() throws Exception {
        AppMapApplicationSettingsService.getInstance().setApiKey("dummy");

        try {
            loadFindingsDirectory("projects/runtime_analysis_tree");

            var model = new RuntimeAnalysisModel(getProject(), getTestRootDisposable());
            var root = model.getRoot();
            assertNotNull("Empty panel must NOT be shown if the user is signed in", root);

            var tempDirName = Path.of(myFixture.getTempDirFixture().getTempDirPath()).getFileName().toString();
            var expected = "-Root\n" +
                    " Findings Table\n" +
                    " -" + tempDirName + "\n" +
                    "  -Failed tests\n" +
                    "   Failed test 1\n" +
                    "   Failed test 2\n" +
                    "  -Findings\n" +
                    "   -More than 30 days ago\n" +
                    "    -Maintainability\n" +
                    "     -Data update performed in GET or HEAD request\n" +
                    "      user.rb\n" +
                    "      user.rb\n" +
                    "      user.rb\n";
            assertTreeHierarchy(model, expected, getTestRootDisposable());
        } finally {
            AppMapApplicationSettingsService.getInstance().setApiKey(null);
        }
    }

    @Test
    public void hierarchy() throws Exception {
        loadFindingsDirectory("projects/runtime_analysis_tree");

        var model = new RuntimeAnalysisModel(getProject(), getTestRootDisposable());
        var tempDirName = Path.of(myFixture.getTempDirFixture().getTempDirPath()).getFileName().toString();
        var expected = "-Root\n" +
                " Findings Table\n" +
                " -" + tempDirName + "\n" +
                "  -Failed tests\n" +
                "   Failed test 1\n" +
                "   Failed test 2\n" +
                "  -Findings\n" +
                "   -More than 30 days ago\n" +
                "    -Maintainability\n" +
                "     -Data update performed in GET or HEAD request\n" +
                "      user.rb\n" +
                "      user.rb\n" +
                "      user.rb\n";
        assertTreeHierarchy(model, expected, getTestRootDisposable());
    }

    @NotNull
    private static List<? extends Node> assertChildren(@NotNull Node findingsRuleNode, int expectedSize) {
        return assertChildren(null, findingsRuleNode, expectedSize);
    }

    @NotNull
    private static List<? extends Node> assertChildren(@Nullable String message,
                                                       @NotNull Node findingsRuleNode,
                                                       int expectedSize) {
        var findingsRuleChildren = findingsRuleNode.getChildren();
        assertEquals(message, expectedSize, findingsRuleChildren.size());
        return findingsRuleChildren;
    }

    private static @NotNull Node assertNode(@NotNull Node node, @NotNull String expectedName) {
        node.update();
        assertEquals(expectedName, node.getName());
        return node;
    }

    private void assertOverviewNode(@NotNull RootNode root) {
        var node = assertNode(root.getChildren().get(0), "Findings Table");
        assertTrue(node instanceof FindingsTableNode);
    }

    private void loadFindingsDirectory(@NotNull String directoryPath) throws Exception {
        var condition = TestFindingsManager.createFindingsCondition(getProject(), getTestRootDisposable());
        WriteAction.computeAndWait(() -> myFixture.copyDirectoryToProject(directoryPath, "root"));
        assertTrue(condition.await(30, TimeUnit.SECONDS));
    }
}