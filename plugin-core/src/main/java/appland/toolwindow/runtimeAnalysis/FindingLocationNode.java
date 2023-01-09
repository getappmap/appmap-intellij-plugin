package appland.toolwindow.runtimeAnalysis;

import appland.problemsView.ScannerProblemWithFile;
import appland.webviews.findingDetails.FindingDetailsEditorProvider;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.ui.tree.LeafState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * Node representing the location of a finding.
 */
class FindingLocationNode extends Node {
    private final @NotNull ScannerProblemWithFile finding;

    public FindingLocationNode(@NotNull Project project,
                               @NotNull NodeDescriptor parentDescriptor,
                               @NotNull ScannerProblemWithFile finding) {
        super(project, parentDescriptor);
        this.finding = finding;
    }

    @Override
    public @NotNull LeafState getLeafState() {
        return LeafState.ALWAYS;
    }

    @Override
    public @Nullable VirtualFile getFile() {
        return finding.getSourceFile();
    }

    @Override
    public @Nullable Navigatable getNavigatable() {
        return new Navigatable() {
            @Override
            public void navigate(boolean requestFocus) {
                FindingDetailsEditorProvider.openEditor(myProject, List.of(finding.getProblem()));
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
        var sourceFile = finding.getSourceFile();
        presentation.setPresentableText(sourceFile != null ? sourceFile.getPresentableName() : "-unknown-");

        var psiFile = findPsiFile();
        if (psiFile != null) {
            var filePresentation = psiFile.getPresentation();
            if (filePresentation != null) {
                presentation.setPresentableText(filePresentation.getPresentableText());
                presentation.setLocationString(filePresentation.getLocationString());
                presentation.setIcon(ReadAction.compute(() -> filePresentation.getIcon(false)));
            }
        }
    }

    @Nullable
    private PsiFile findPsiFile() {
        var file = finding.getSourceFile();
        if (file == null) {
            return null;
        }
        return ReadAction.compute(() -> PsiManager.getInstance(myProject).findFile(file));
    }
}
