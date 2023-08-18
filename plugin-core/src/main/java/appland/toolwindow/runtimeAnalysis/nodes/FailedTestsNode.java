package appland.toolwindow.runtimeAnalysis.nodes;

import appland.AppMapBundle;
import appland.index.AppMapMetadata;
import appland.index.AppMapMetadataService;
import appland.problemsView.model.TestStatus;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.tree.LeafState;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Node showing AppMaps with test status "Failed".
 * Because it must display AppMaps with and without findings, we're not reusing the problems
 * of the parent node. These problems are only covering AppMaps with findings attached.
 */
final class FailedTestsNode extends Node {
    private final @NotNull List<AppMapMetadata> failedAppMaps;

    public FailedTestsNode(@NotNull Project project,
                           @NotNull NodeDescriptor parentDescriptor,
                           @NotNull List<AppMapMetadata> failedAppMaps) {
        super(project, parentDescriptor);
        this.failedAppMaps = failedAppMaps;
    }

    @Override
    public @NotNull LeafState getLeafState() {
        return LeafState.NEVER;
    }

    @Override
    protected void update(@NotNull PresentationData presentation) {
        presentation.setPresentableText(AppMapBundle.get("runtimeAnalysis.node.failedTests.label"));
    }

    @Override
    public List<? extends Node> getChildren() {
        return failedAppMaps.stream()
                .filter(appMapMetadata -> appMapMetadata.getAppMapFile() != null)
                .map(appMapMetadata -> new AppMapNode(myProject, this, appMapMetadata.getAppMapFile()))
                .sorted(Comparator.comparing(AppMapNode::getTitle))
                .collect(Collectors.toList());
    }

    static List<AppMapMetadata> findFailedAppMaps(@NotNull Project project) {
        return ReadAction.compute(() -> AppMapMetadataService.getInstance(project).findAppMaps()
                .stream()
                .filter(data -> data.getTestStatus() == TestStatus.Failed)
                .collect(Collectors.toList()));
    }
}
