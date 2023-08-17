package appland.toolwindow.runtimeAnalysis.nodes;

import appland.problemsView.ScannerProblem;
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
 * Node representing a single findings and its location.
 */
final class FindingLocationNode extends Node {
    private final @NotNull ScannerProblem problem;

    public FindingLocationNode(@NotNull Project project,
                               @NotNull NodeDescriptor parentDescriptor,
                               @NotNull ScannerProblem problem) {
        super(project, parentDescriptor);
        this.problem = problem;
    }

    @Override
    public @NotNull LeafState getLeafState() {
        return LeafState.ALWAYS;
    }

    @Override
    public @NotNull VirtualFile getFile() {
        return problem.getFile();
    }

    @Override
    public @NotNull Navigatable getNavigatable() {
        return new Navigatable() {
            @Override
            public void navigate(boolean requestFocus) {
                FindingDetailsEditorProvider.openEditor(myProject, List.of(problem));
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
        presentation.setPresentableText(problem.getFile().getPresentableName());

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
        return ReadAction.compute(() -> PsiManager.getInstance(myProject).findFile(problem.getFile()));
    }
}
