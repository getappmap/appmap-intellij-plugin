package appland.toolwindow.runtimeAnalysis.nodes;

import appland.problemsView.ScannerProblem;
import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.ui.tree.LeafState;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Findings of a particular AppMap project.
 * The child nodes are "Findings" and "Failed tests".
 */
final class AppMapProjectFindingsNode extends Node {
    private final String projectName;
    private final List<ScannerProblem> problems;

    public AppMapProjectFindingsNode(@NotNull Project project,
                                     @NotNull NodeDescriptor parentDescriptor,
                                     @NotNull String projectName,
                                     @NotNull List<ScannerProblem> problems) {
        super(project, parentDescriptor);
        this.projectName = projectName;
        this.problems = problems;
    }

    @Override
    protected void update(@NotNull PresentationData presentation) {
        presentation.setPresentableText(projectName);
        presentation.setIcon(AllIcons.Nodes.Folder);
    }

    @Override
    public List<? extends Node> getChildren() {
        if (problems.isEmpty()) {
            return Collections.emptyList();
        }

        var nodes = new ArrayList<Node>();

        var failedAppMaps = FailedTestsNode.findFailedAppMaps(myProject);
        if (failedAppMaps != null && !failedAppMaps.isEmpty()) {
            nodes.add(new FailedTestsNode(myProject, this, failedAppMaps));
        }

        nodes.add(new FailedAndSuccessfulFindingsNode(myProject, this, problems));

        return nodes;
    }

    @Override
    public @NotNull LeafState getLeafState() {
        return LeafState.NEVER;
    }
}
