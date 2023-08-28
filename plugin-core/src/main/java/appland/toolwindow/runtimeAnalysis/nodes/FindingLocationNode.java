package appland.toolwindow.runtimeAnalysis.nodes;

import appland.problemsView.model.ScannerFinding;
import appland.webviews.findingDetails.FindingDetailsEditorProvider;
import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.pom.Navigatable;
import com.intellij.ui.tree.LeafState;
import com.intellij.util.PathUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * Node representing a single findings and its location.
 */
final class FindingLocationNode extends Node {
    private final @NotNull ScannerFinding finding;

    public FindingLocationNode(@NotNull Project project,
                               @NotNull NodeDescriptor parentDescriptor,
                               @NotNull ScannerFinding finding) {
        super(project, parentDescriptor);
        this.finding = finding;
    }

    @Override
    public @NotNull LeafState getLeafState() {
        return LeafState.ALWAYS;
    }

    @Override
    public @NotNull Navigatable getNavigatable() {
        return new Navigatable() {
            @Override
            public void navigate(boolean requestFocus) {
                FindingDetailsEditorProvider.openEditor(myProject, List.of(finding));
            }

            @Override
            public boolean canNavigate() {
                return true;
            }

            @Override
            public boolean canNavigateToSource() {
                return false;
            }
        };
    }

    @Override
    public List<? extends Node> getChildren() {
        return Collections.emptyList();
    }

    @Override
    protected void update(@NotNull PresentationData presentation) {
        var event = finding.event;

        if (finding.getProblemLocationFromStack() != null) {
            var filePath = finding.getProblemLocationFromStack().filePath;
            var fileName = PathUtil.getFileName(filePath);
            presentation.setPresentableText(fileName);

            var fileType = FileTypeManager.getInstance().getFileTypeByFileName(fileName);
            presentation.setIcon(fileType.getIcon());

            presentation.setLocationString(filePath);
        } else if (event != null) {
            if (event.path != null) {
                presentation.setPresentableText(event.path);
            } else if (event.httpServerRequest != null) {
                var request = event.httpServerRequest;
                presentation.setPresentableText(String.format("%s %s", request.requestMethod, request.pathInfo));
            }
        }
    }
}
