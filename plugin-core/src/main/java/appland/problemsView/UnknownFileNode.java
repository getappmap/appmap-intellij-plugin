package appland.problemsView;

import appland.Icons;
import appland.problemsView.model.ScannerFinding;
import com.intellij.analysis.problemsView.toolWindow.Node;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.openapi.project.Project;
import com.intellij.pom.Navigatable;
import com.intellij.ui.tree.LeafState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.ui.SimpleTextAttributes.REGULAR_ATTRIBUTES;

class UnknownFileNode extends Node {
    private final ScannerFinding finding;

    UnknownFileNode(@NotNull Node parent, @NotNull ScannerFinding finding) {
        super(parent);
        this.finding = finding;
    }

    @Override
    public int hashCode() {
        return finding.hashCode();
    }

    @NotNull
    @Override
    public String getName() {
        return finding.getFindingTitle();
    }

    @Override
    public @NotNull LeafState getLeafState() {
        return LeafState.ALWAYS;
    }

    @Nullable
    @Override
    public Navigatable getNavigatable() {
        return new UnknownFileNavigatable(myProject, finding);
    }

    @Override
    protected void update(@NotNull Project project, @NotNull PresentationData presentation) {
        presentation.addText(finding.getFindingTitle(), REGULAR_ATTRIBUTES);
        presentation.setIcon(Icons.APPMAP_FILE_SMALL);
    }
}
