package appland.toolwindow.runtimeAnalysis.nodes;

import appland.problemsView.model.ScannerFinding;
import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.ui.tree.LeafState;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Node displaying a date bucket, e.g. "Last 30 days".
 * It creates child nodes grouped by impact domain.
 */
final class DateBucketNode extends Node {
    private final String label;
    private final List<ScannerFinding> findings;

    public DateBucketNode(@NotNull Project project,
                          @NotNull NodeDescriptor parentDescriptor,
                          @NotNull String label,
                          @NotNull List<ScannerFinding> findings) {
        super(project, parentDescriptor);
        this.label = label;
        this.findings = findings;
    }

    @Override
    public @NotNull LeafState getLeafState() {
        return LeafState.NEVER;
    }

    @Override
    protected void update(@NotNull PresentationData presentation) {
        presentation.setPresentableText(label);
        presentation.setIcon(AllIcons.Actions.GroupBy);
    }

    @Override
    public List<? extends Node> getChildren() {
        var byImpactDomain = findings.stream()
                .filter(problem -> problem.impactDomain != null)
                .collect(Collectors.groupingBy(
                        finding -> finding.impactDomain,
                        TreeMap::new,
                        Collectors.toList()));

        // group by impact domain
        return byImpactDomain.entrySet().stream()
                .map(entry -> new ImpactDomainNode(myProject, this, entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }
}
