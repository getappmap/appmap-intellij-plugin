package appland.toolwindow.runtimeAnalysis;

import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.ide.util.treeView.PresentableNodeDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.ui.tree.LeafState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

abstract class Node extends PresentableNodeDescriptor<Node> implements LeafState.Supplier {
    protected Node(@NotNull Project project, @Nullable NodeDescriptor parentDescriptor) {
        super(project, parentDescriptor);
    }

    @Override
    public final Node getElement() {
        return this;
    }

    /**
     * @return List of child nodes or an empty collection for leaf nodes.
     */
    public abstract List<? extends Node> getChildren();

    /**
     * @return {@link VirtualFile} associated with this node.
     * {@code null} if it's a node without a file.
     */
    public @Nullable VirtualFile getFile() {
        return null;
    }

    /**
     * @return A {@link Navigatable}, which is called when the node was executed
     * (e.g. by double-clicking). {@code null} if the node is not navigable.
     */
    public @Nullable Navigatable getNavigatable() {
        return null;
    }
}
