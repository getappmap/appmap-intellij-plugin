package appland.toolwindow.runtimeAnalysis.nodes;

import appland.problemsView.model.ImpactDomain;
import appland.problemsView.model.ScannerFinding;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.ui.tree.LeafState;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Node representing an impact domain.
 * Child nodes are the actual findings of this domain.
 */
public final class ImpactDomainNode extends Node {
    private final @NotNull ImpactDomain impactDomain;
    private final @NotNull List<ScannerFinding> domainFindings;

    public ImpactDomainNode(@NotNull Project project,
                            @NotNull NodeDescriptor parentDescriptor,
                            @NotNull ImpactDomain impactDomain,
                            @NotNull List<ScannerFinding> domainFindings) {
        super(project, parentDescriptor);
        this.impactDomain = impactDomain;
        this.domainFindings = domainFindings;
    }

    @Override
    public @NotNull LeafState getLeafState() {
        return LeafState.NEVER;
    }

    @Override
    public List<? extends Node> getChildren() {
        if (domainFindings.isEmpty()) {
            return Collections.emptyList();
        }

        var byRuleTitle = domainFindings.stream().collect(Collectors.groupingBy(problem -> {
            var ruleTitle = problem.ruleTitle;
            return ruleTitle.isEmpty() ? "Unknown" : ruleTitle;
        }));

        return byRuleTitle.entrySet().stream()
                .map(entry -> new FindingsGroupNode(myProject, this, entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    @Override
    protected void update(@NotNull PresentationData presentation) {
        presentation.setPresentableText(impactDomain.name());
    }
}
