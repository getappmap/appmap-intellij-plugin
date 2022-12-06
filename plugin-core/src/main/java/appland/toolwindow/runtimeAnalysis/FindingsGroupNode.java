package appland.toolwindow.runtimeAnalysis;

import appland.problemsView.ScannerProblemWithFile;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.ui.tree.LeafState;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Node representing a unique finding title.
 * Child nodes are the actual locations of this finding.
 */
class FindingsGroupNode extends Node {
    private final @NotNull String title;
    private final @NotNull List<FindingLocationNode> findings;

    public FindingsGroupNode(@NotNull Project project,
                             @NotNull NodeDescriptor parentDescriptor,
                             @NotNull String title,
                             @NotNull List<ScannerProblemWithFile> findings) {
        super(project, parentDescriptor);
        this.title = title;
        this.findings = deduplicateFindings(findings)
                .stream()
                .map(finding -> new FindingLocationNode(project, this, finding))
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public @NotNull LeafState getLeafState() {
        return LeafState.NEVER;
    }

    @Override
    public List<? extends Node> getChildren() {
        return findings;
    }

    @Override
    protected void update(@NotNull PresentationData presentation) {
        presentation.setPresentableText(title);
    }

    /**
     * Deduplicate findings by AppMap hash code.
     */
    @NotNull
    private static List<ScannerProblemWithFile> deduplicateFindings(@NotNull List<ScannerProblemWithFile> findings) {
        var foundFindingHashes = new HashSet<String>();

        var findingNodes = new ArrayList<ScannerProblemWithFile>();
        for (var finding : findings) {
            var hashCode = finding.getProblem().getFinding().getAppMapHashWithFallback();
            if (hashCode != null && !hashCode.isEmpty()) {
                if (!foundFindingHashes.contains(hashCode)) {
                    foundFindingHashes.add(hashCode);
                    findingNodes.add(finding);
                }
            }
        }
        return findingNodes;
    }
}
