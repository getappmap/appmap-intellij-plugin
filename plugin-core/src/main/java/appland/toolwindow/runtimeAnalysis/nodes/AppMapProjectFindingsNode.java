package appland.toolwindow.runtimeAnalysis.nodes;

import appland.problemsView.model.ScannerFinding;
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
    private final @NotNull List<ScannerFinding> findings;

    public AppMapProjectFindingsNode(@NotNull Project project,
                                     @NotNull NodeDescriptor parentDescriptor,
                                     @NotNull String projectName,
                                     @NotNull List<ScannerFinding> findings) {
        super(project, parentDescriptor);
        this.projectName = projectName;
        this.findings = findings;
    }

    @Override
    protected void update(@NotNull PresentationData presentation) {
        presentation.setPresentableText(projectName);
        presentation.setIcon(AllIcons.Nodes.Folder);
    }

    @Override
    public List<? extends Node> getChildren() {
        if (findings.isEmpty()) {
            return Collections.emptyList();
        }

        var nodes = new ArrayList<Node>();

        var failedAppMaps = FailedTestsNode.findFailedAppMaps(myProject);
        if (failedAppMaps != null && !failedAppMaps.isEmpty()) {
            nodes.add(new FailedTestsNode(myProject, this, failedAppMaps));
        }

        nodes.add(new FailedAndSuccessfulFindingsNode(myProject, this, findings));

        return nodes;
    }

    @Override
    public @NotNull LeafState getLeafState() {
        return LeafState.NEVER;
    }
}
