package appland.toolwindow.appmap;

import appland.toolwindow.appmap.nodes.Node;
import appland.toolwindow.appmap.nodes.RootNode;
import com.intellij.openapi.project.Project;
import com.intellij.ui.tree.BaseTreeModel;
import com.intellij.util.concurrency.Invoker;
import com.intellij.util.concurrency.InvokerSupplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class AppMapModel extends BaseTreeModel<Node> implements InvokerSupplier {
    private final RootNode root;
    private final Invoker invoker = Invoker.forBackgroundPoolWithoutReadAction(this);
    private final AtomicReference<String> nameFilter = new AtomicReference<>();

    public AppMapModel(@NotNull Project project) {
        this.root = new RootNode(project, this);
    }

    public @Nullable String getNameFilter() {
        return nameFilter.get();
    }

    public void setNameFilter(@Nullable String name) {
        var old = nameFilter.getAndSet(name);
        if (!Objects.equals(old, name)) {
            refresh();
        }
    }

    @Override
    public boolean isLeaf(Object object) {
        return root != object && super.isLeaf(object);
    }

    public void refresh() {
        root.queueAppMapRefresh();
        treeStructureChanged(null, null, null);
    }

    @Override
    public @NotNull Invoker getInvoker() {
        return invoker;
    }

    @Override
    public @Nullable RootNode getRoot() {
        if (invoker.isValidThread()) {
            root.update();
        }
        return root;
    }

    @Override
    public List<? extends Node> getChildren(Object parent) {
        assert invoker.isValidThread();

        var node = parent instanceof Node ? (Node) parent : null;
        if (node == null) {
            return Collections.emptyList();
        }

        var childNodes = node.getChildren();
        node.update();
        childNodes.forEach(Node::update);
        return childNodes;
    }
}
