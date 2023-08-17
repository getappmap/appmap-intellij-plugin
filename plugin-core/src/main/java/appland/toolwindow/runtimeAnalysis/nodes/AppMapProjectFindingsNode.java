package appland.toolwindow.runtimeAnalysis.nodes;

import appland.problemsView.ScannerProblem;
import appland.problemsView.model.TestStatus;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.ui.tree.LeafState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Findings of a particular AppMap project.
 * The child nodes are "Findings" and "Failed tests".
 */
final class AppMapProjectFindingsNode extends Node {
    private final String projectName;
    private final List<ScannerProblem> problems;

    public AppMapProjectFindingsNode(@NotNull Project project,
                                     @Nullable NodeDescriptor parentDescriptor,
                                     @NotNull String projectName,
                                     List<ScannerProblem> problems) {
        super(project, parentDescriptor);
        this.projectName = projectName;
        this.problems = problems;
    }

    @Override
    protected void update(@NotNull PresentationData presentation) {
        presentation.setPresentableText(projectName);
    }

    @Override
    public List<? extends Node> getChildren() {
        @SuppressWarnings("DataFlowIssue")
        var byTestStatus = problems.stream()
                .filter(item -> {
                    var metaData = item.getFinding().getFindingsMetaData();
                    return metaData != null && metaData.testStatus != null;
                })
                .collect(Collectors.groupingBy(item -> item.getFinding().getFindingsMetaData().testStatus));

        if (byTestStatus.isEmpty()) {
            return Collections.emptyList();
        }

        var nodes = new ArrayList<Node>();

        var failedAppMaps = FailedTestsNode.findFailedAppMaps(myProject);
        if (failedAppMaps != null && !failedAppMaps.isEmpty()) {
            nodes.add(new FailedTestsNode(myProject, this, failedAppMaps));
        }

        var successfulTests = byTestStatus.get(TestStatus.Succeeded);
        if (successfulTests != null && !successfulTests.isEmpty()) {
            nodes.add(new SuccessfulTestsNode(myProject, this, successfulTests));
        }

        return nodes;
    }

    @Override
    public @NotNull LeafState getLeafState() {
        return LeafState.NEVER;
    }
}
