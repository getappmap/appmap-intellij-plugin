package appland.toolwindow.runtimeAnalysis;

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

class OverviewNode extends Node implements Navigatable {
    protected OverviewNode(@NotNull Project project, @NotNull NodeDescriptor parentDescriptor) {
        super(project, parentDescriptor);
    }

    @Override
    protected void update(@NotNull PresentationData presentation) {
        presentation.setAttributesKey(CodeInsightColors.HYPERLINK_ATTRIBUTES);
        presentation.setPresentableText("Overview");
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
