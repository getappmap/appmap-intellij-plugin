package appland.problemsView;

import appland.problemsView.listener.ScannerFindingsListener;
import com.intellij.analysis.problemsView.Problem;
import com.intellij.analysis.problemsView.toolWindow.Node;
import com.intellij.analysis.problemsView.toolWindow.ProblemsViewPanel;
import com.intellij.analysis.problemsView.toolWindow.Root;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

public class FindingsRootNode extends Root implements ScannerFindingsListener {
    private final @NotNull FindingsManager manager;

    public FindingsRootNode(@NotNull ProblemsViewPanel panel) {
        super(panel);
        this.manager = FindingsManager.getInstance(getProject());

        var busConnection = panel.getProject().getMessageBus().connect(this);
        busConnection.subscribe(ScannerFindingsListener.TOPIC, this);
    }

    @Override
    public void problemAppeared(@NotNull Problem problem) {
        assert problem instanceof ScannerProblem;
        super.problemAppeared(problem);
    }

    @Override
    public void problemDisappeared(@NotNull Problem problem) {
        assert problem instanceof ScannerProblem;
        super.problemDisappeared(problem);
    }

    @Override
    public void problemUpdated(@NotNull Problem problem) {
        assert problem instanceof ScannerProblem;
        super.problemUpdated(problem);
    }

    @Override
    public void afterUnknownFileProblemsChange() {
        structureChanged(null);
    }

    @Override
    public int getFileProblemCount(@NotNull VirtualFile virtualFile) {
        return manager.getProblemCount(virtualFile);
    }

    @NotNull
    @Override
    @SuppressWarnings("unchecked")
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

    @NotNull
    @Override
    public Collection<Node> getChildren() {
        var children = super.getChildren();

        if (manager.getOtherProblemCount() > 0) {
            children.add(new UnknownFileGroupNode(this));
        }

        return children;
    }
}
