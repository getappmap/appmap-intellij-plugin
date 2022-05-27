package appland.problemsView;

import appland.AppMapBundle;
import com.intellij.analysis.problemsView.toolWindow.Node;
import com.intellij.analysis.problemsView.toolWindow.ProblemsViewPanel;
import com.intellij.analysis.problemsView.toolWindow.ProblemsViewState;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;

public class FindingsViewTab extends ProblemsViewPanel {
    public FindingsViewTab(@NotNull Project project, @NotNull ProblemsViewState state) {
        super(project, "appmap.scannerView", state, AppMapBundle.lazy("problemsView.title"));

        getTreeModel().setRoot(new FindingsRootNode(this));

        DumbService.getInstance(project).runWhenSmart(() -> {
            FindingsManager.getInstance(project).loadAllFindingFiles();
        });
    }

    /**
     * @return A comparator, which always sorts the "Unknown files" node to the top
     */
    @Override
    protected @NotNull Comparator<Node> createComparator() {
        var original = super.createComparator();
        return (a, b) -> {
            if (a instanceof UnknownFileGroupNode) {
                return -1;
            }
            if (b instanceof UnknownFileGroupNode) {
                return 1;
            }
            return original.compare(a, b);
        };
    }
}
