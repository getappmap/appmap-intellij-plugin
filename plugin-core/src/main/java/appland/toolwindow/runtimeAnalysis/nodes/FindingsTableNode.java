package appland.toolwindow.runtimeAnalysis.nodes;

import appland.AppMapBundle;
import appland.webviews.findings.FindingsOverviewEditorProvider;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.editor.colors.CodeInsightColors;
import com.intellij.openapi.project.Project;
import com.intellij.pom.Navigatable;
import com.intellij.ui.tree.LeafState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public final class FindingsTableNode extends Node implements Navigatable {
    protected FindingsTableNode(@NotNull Project project, @NotNull NodeDescriptor parentDescriptor) {
        super(project, parentDescriptor);
    }

    @Override
    protected void update(@NotNull PresentationData presentation) {
        presentation.setAttributesKey(CodeInsightColors.HYPERLINK_ATTRIBUTES);
        presentation.setPresentableText(AppMapBundle.get("runtimeAnalysis.node.findingsTable.label"));
    }

    @Override
    public @NotNull LeafState getLeafState() {
        return LeafState.ALWAYS;
    }

    @Override
    public List<? extends Node> getChildren() {
        return Collections.emptyList();
    }

    @Override
    public @Nullable Navigatable getNavigatable() {
        return this;
    }

    @Override
    public boolean canNavigate() {
        return true;
    }

    @Override
    public boolean canNavigateToSource() {
        return false;
    }

    @Override
    public void navigate(boolean requestFocus) {
        FindingsOverviewEditorProvider.openEditor(myProject);
    }
}
