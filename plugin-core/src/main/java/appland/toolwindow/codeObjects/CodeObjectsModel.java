package appland.toolwindow.codeObjects;

import com.intellij.openapi.Disposable;
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
import java.util.List;

class CodeObjectsModel extends BaseTreeModel<Node> implements InvokerSupplier {
    @Getter
    private final @NotNull Invoker invoker = Invoker.forBackgroundPoolWithReadAction(this);
    private final @NotNull RootNode root;

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

    public void structureChanged(@Nullable TreePath path) {
        treeStructureChanged(path, null, null);
    }
}
