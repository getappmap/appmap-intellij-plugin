package appland.toolwindow.runtimeAnalysis;

import appland.problemsView.ScannerProblemWithFile;
import appland.problemsView.model.ImpactDomain;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.ui.tree.LeafState;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Node representing an impact domain.
 * Child nodes are the actual findings of this domain.
 */
class ImpactDomainNode extends Node {
    private final @NotNull ImpactDomain impactDomain;
    private final @NotNull List<ScannerProblemWithFile> domainFindings;

    public ImpactDomainNode(@NotNull Project project,
                            @NotNull NodeDescriptor parentDescriptor,
                            @NotNull ImpactDomain impactDomain,
                            @NotNull List<ScannerProblemWithFile> domainFindings) {
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

        // group by finding title
        Map<String, List<ScannerProblemWithFile>> byTitle = new HashMap<>();
        for (var finding : domainFindings) {
            var title = finding.getProblem().getFinding().ruleTitle;
            if (!title.isEmpty()) {
                byTitle.merge(title, List.of(finding), (a, b) -> {
                    var merged = new ArrayList<ScannerProblemWithFile>(a.size() + b.size());
                    merged.addAll(a);
                    merged.addAll(b);
                    return merged;
                });
            }
        }
        return byTitle.entrySet()
                .stream()
                .map(entry -> new FindingsGroupNode(myProject, this, entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    @Override
    protected void update(@NotNull PresentationData presentation) {
        presentation.setPresentableText(impactDomain.name());
    }
}
