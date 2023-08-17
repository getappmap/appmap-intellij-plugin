package appland.toolwindow.runtimeAnalysis.nodes;

import appland.problemsView.ScannerProblem;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.ui.tree.LeafState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

final class DateBucketNode extends Node {
    private final String label;
    private final List<ScannerProblem> problems;

    public DateBucketNode(@NotNull Project project,
                          @Nullable NodeDescriptor parentDescriptor,
                          @NotNull String label,
                          List<ScannerProblem> problems) {
        super(project, parentDescriptor);
        this.label = label;
        this.problems = problems;
    }

    @Override
    public @NotNull LeafState getLeafState() {
        return LeafState.NEVER;
    }

    @Override
    protected void update(@NotNull PresentationData presentation) {
        presentation.setPresentableText(label);
    }

    @Override
    public List<? extends Node> getChildren() {
        var byImpactDomain = problems.stream()
                .filter(problem -> problem.getFinding().impactDomain != null)
                .collect(Collectors.groupingBy(
                        item -> item.getFinding().impactDomain,
                        TreeMap::new,
                        Collectors.toList()));

        // group by impact domain
        return byImpactDomain.entrySet().stream()
                .map(entry -> new ImpactDomainNode(myProject, this, entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }
}
