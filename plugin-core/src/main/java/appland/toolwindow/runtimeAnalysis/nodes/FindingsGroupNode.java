package appland.toolwindow.runtimeAnalysis.nodes;

import appland.problemsView.model.ScannerFinding;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.ui.tree.LeafState;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Node representing a unique finding title.
 * Child nodes are the actual locations of this finding.
 */
final class FindingsGroupNode extends Node {
    private final @NotNull String title;
    private final @NotNull List<FindingLocationNode> findings;

    public FindingsGroupNode(@NotNull Project project,
                             @NotNull NodeDescriptor parentDescriptor,
                             @NotNull String title,
                             @NotNull List<ScannerFinding> findings) {
        super(project, parentDescriptor);
        this.title = title;
        this.findings = deduplicateFindings(findings)
                .stream()
                .sorted(Comparator.comparing(Object::toString))
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
    private static List<ScannerFinding> deduplicateFindings(@NotNull List<ScannerFinding> problems) {
        var foundFindingHashes = new HashSet<String>();
        var findingNodes = new ArrayList<ScannerFinding>();

        // deduplicate after sorting to get a reliable result for tests
        problems.stream()
                .sorted(Comparator.comparing(finding -> {
                    var file = finding.getFindingsFile();
                    return file != null ? file.getPath() : finding.getFindingTitle();
                }))
                .forEachOrdered(problem -> {
                    var hashCode = problem.getAppMapHashWithFallback();
                    if (hashCode != null && !hashCode.isEmpty()) {
                        if (!foundFindingHashes.contains(hashCode)) {
                            foundFindingHashes.add(hashCode);
                            findingNodes.add(problem);
                        }
                    }
                });

        return findingNodes;
    }
}
