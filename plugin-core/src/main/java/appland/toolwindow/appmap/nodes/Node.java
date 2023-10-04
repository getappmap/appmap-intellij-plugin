package appland.toolwindow.appmap.nodes;

import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.ide.util.treeView.PresentableNodeDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.tree.LeafState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public abstract class Node extends PresentableNodeDescriptor<Node> implements LeafState.Supplier {
    public Node(@NotNull Project project, @Nullable NodeDescriptor parentDescriptor) {
        super(project, parentDescriptor);
    }

    @Override
    public Node getElement() {
        return this;
    }

    /**
     * @return List of child nodes or an empty collection for leaf nodes.
     */
    public abstract List<? extends Node> getChildren();

    /**
     * @return {@link VirtualFile} associated with this node. {@code null} if it's a node without a file.
     */
    public @Nullable VirtualFile getFile() {
        return null;
    }

    /**
     * @return List of {@link VirtualFile} associated with this node or an empty list if no files are associated.
     */
    public @NotNull List<VirtualFile> getFiles() {
        var file = getFile();
        return file != null ? List.of(file) : Collections.emptyList();
    }
}
