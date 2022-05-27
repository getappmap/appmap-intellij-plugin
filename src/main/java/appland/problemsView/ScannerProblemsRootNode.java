package appland.problemsView;

import com.intellij.analysis.problemsView.Problem;
import com.intellij.analysis.problemsView.ProblemsListener;
import com.intellij.analysis.problemsView.toolWindow.ProblemsViewPanel;
import com.intellij.analysis.problemsView.toolWindow.Root;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

public class ScannerProblemsRootNode extends Root {
    private final @NotNull ScannerProblemsManager manager;

    public ScannerProblemsRootNode(@NotNull ProblemsViewPanel panel) {
        super(panel);
        this.manager = ScannerProblemsManager.getInstance(getProject());
        panel.getProject().getMessageBus().connect(this).subscribe(ProblemsListener.TOPIC, this);
    }

    @Override
    public void problemAppeared(@NotNull Problem problem) {
        if (!(problem instanceof ScannerProblem)) {
            return;
        }

        super.problemAppeared(problem);
    }

    @Override
    public void problemDisappeared(@NotNull Problem problem) {
        if (!(problem instanceof ScannerProblem)) {
            return;
        }

        super.problemDisappeared(problem);
    }

    @Override
    public void problemUpdated(@NotNull Problem problem) {
        if (!(problem instanceof ScannerProblem)) {
            return;
        }

        super.problemUpdated(problem);
    }

    @Override
    public int getFileProblemCount(@NotNull VirtualFile virtualFile) {
        return manager.getProblemCount(virtualFile);
    }

    @NotNull
    @Override
    public Collection<Problem> getFileProblems(@NotNull VirtualFile virtualFile) {
        return manager.getProblems(virtualFile);
    }

    @Override
    public int getOtherProblemCount() {
        return 0;
    }

    @NotNull
    @Override
    public Collection<Problem> getOtherProblems() {
        return Collections.emptyList();
    }

    @Override
    public int getProblemCount() {
        return manager.getProblemFileCount();
    }

    @NotNull
    @Override
    public Collection<VirtualFile> getProblemFiles() {
        return manager.getProblemFiles();
    }
}
