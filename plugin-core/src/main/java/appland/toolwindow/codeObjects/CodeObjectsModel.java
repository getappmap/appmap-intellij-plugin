package appland.toolwindow.codeObjects;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.tree.BaseTreeModel;
import com.intellij.util.concurrency.Invoker;
import com.intellij.util.concurrency.InvokerSupplier;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.TreePath;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

class CodeObjectsModel extends BaseTreeModel<Node> implements InvokerSupplier {
    @Getter
    private final @NotNull Invoker invoker = Invoker.forBackgroundPoolWithReadAction(this);
    private final @NotNull RootNode root;

    // compare HTTP route nodes first by path, then by method. Everything else is sorted by display name.
    // Finally sort by weight to get a stable sort order, e.g. for tests.
    private final @NotNull Comparator<Node> nodeComparator = ((Comparator<Node>) (a, b) -> {
        if (a instanceof HttpRouteNode && b instanceof HttpRouteNode) {
            if (((HttpRouteNode) a).getHttpPath().equals(((HttpRouteNode) b).getHttpPath())) {
                return ((HttpRouteNode) a).getHttpPath().compareToIgnoreCase(((HttpRouteNode) b).getHttpPath());
            }
            return String.CASE_INSENSITIVE_ORDER.compare(((HttpRouteNode) a).getHttpPath(), ((HttpRouteNode) b).getHttpPath());
        }
        return 0;
    }).thenComparing(Node::getDisplayName, String::compareToIgnoreCase).thenComparing(Node::getWeight);

    public CodeObjectsModel(@NotNull Project project, @NotNull Disposable parent) {
        this.root = new RootNode(project, this);
        Disposer.register(parent, this);

        project.getMessageBus().connect(this).subscribe(DumbService.DUMB_MODE, new DumbService.DumbModeListener() {
            @Override
            public void exitDumbMode() {
                structureChanged(null);
            }
        });
    }

    @Override
    public Object getRoot() {
        if (invoker.isValidThread()) {
            root.update();
        }
        return root;
    }

    @Override
    public List<? extends Node> getChildren(Object parent) {
        assert ApplicationManager.getApplication().isUnitTestMode() || invoker.isValidThread();

        var node = parent instanceof Node ? (Node) parent : null;
        if (node == null) {
            return Collections.emptyList();
        }

        node.update();
        var childNodes = node.getChildren().stream().sorted(nodeComparator).collect(Collectors.toList());
        childNodes.forEach(Node::update);
        return childNodes;
    }

    public void structureChanged(@Nullable TreePath path) {
        treeStructureChanged(path, null, null);
    }
}
