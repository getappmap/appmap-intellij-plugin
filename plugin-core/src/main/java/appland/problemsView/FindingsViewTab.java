package appland.problemsView;

import appland.AppMapBundle;
import appland.index.IndexedFileListenerUtil;
import com.intellij.analysis.problemsView.toolWindow.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;

public final class FindingsViewTab extends ProblemsViewPanel {
    private static final String TAB_ID = "appmap.scannerView";

    /**
     * Show and activate the AppMap findings tab.
     *
     * @see ProblemsView#toggleCurrentFileProblems(Project, VirtualFile)
     */
    public static void activateFindingsTab(@NotNull Project project) {
        var window = ProblemsView.getToolWindow(project);
        if (window == null) {
            return;
        }

        window.activate(() -> {
            var contentManager = window.getContentManager();
            ProblemsViewToolWindowUtils.INSTANCE.selectContent(contentManager, TAB_ID);
        }, true, true);
    }

    public FindingsViewTab(@NotNull Project project, @NotNull ProblemsViewState state) {
        super(project, TAB_ID, state, AppMapBundle.lazy("problemsView.title"));

        var rootNode = new FindingsRootNode(this);
        getTreeModel().setRoot(rootNode);

        IndexedFileListenerUtil.registerListeners(project, this, false, true, false, () -> {
            rootNode.structureChanged(null);
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
