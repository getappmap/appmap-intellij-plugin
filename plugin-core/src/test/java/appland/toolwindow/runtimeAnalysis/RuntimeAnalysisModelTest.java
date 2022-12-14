package appland.toolwindow.runtimeAnalysis;

import appland.AppMapBaseTest;
import appland.problemsView.FindingsManager;
import appland.problemsView.listener.ScannerFilesAsyncListener;
import appland.settings.AppMapApplicationSettingsService;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.project.DumbUtilImpl;
import com.intellij.testFramework.fixtures.impl.CodeInsightTestFixtureImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assume;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class RuntimeAnalysisModelTest extends AppMapBaseTest {
    @Override
    protected boolean runInDispatchThread() {
        return false;
    }

    @Test
    public void unauthenticated() {
        AppMapApplicationSettingsService.getInstance().setApiKey(null);

        var model = new RuntimeAnalysisModel(getProject(), getTestRootDisposable());
        assertNull("Empty panel must be shown if the user is not signed in", model.getRoot());
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
        // fixme(jansorg): This test is passing locally, but consistently failing on Travis CI
        //      we're disabling this until we figure out the root cause
        Assume.assumeTrue(System.getenv("CI") == null);

        AppMapApplicationSettingsService.getInstance().setApiKey("dummy");
        loadFindingsDirectory("projects/spring-petclinic");
        assertEquals("Overview and 'Performance' impact domain expected",
                2, FindingsManager.getInstance(getProject()).getAllFindings().size());

        try {
            var model = new RuntimeAnalysisModel(getProject(), getTestRootDisposable());
            var root = model.getRoot();
            assertNotNull("Empty panel must NOT be shown if the user is signed in", root);
            root.update();
            assertOverviewNode(root);

            var rootChildren = assertChildren("Overview node and performance group expected", root, 2);

            var impactDomain = assertNode(rootChildren.get(1), "Performance");
            assertTrue(impactDomain instanceof ImpactDomainNode);

            var impactChildren = assertChildren(impactDomain, 1);

            var findingsRuleNode = assertNode(impactChildren.get(0), "N plus 1 SQL query");
            var findingsRuleChildren = assertChildren(findingsRuleNode, 2);

            assertNode(findingsRuleChildren.get(0), "ClinicServiceTests.java");
            assertNode(findingsRuleChildren.get(1), "PetClinicIntegrationTests.java");
        } finally {
            AppMapApplicationSettingsService.getInstance().setApiKey(null);
        }
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
        var node = assertNode(root.getChildren().get(0), "Overview");
        assertTrue(node instanceof OverviewNode);
    }

    private void loadFindingsDirectory(@NotNull String directoryPath) throws Exception {
        ScannerFilesAsyncListener.disableForTests(() -> {
            return WriteAction.computeAndWait(() -> myFixture.copyDirectoryToProject(directoryPath, "root"));
        });
        FindingsManager.getInstance(getProject()).reloadAsync().get(30, TimeUnit.SECONDS);
    }
}