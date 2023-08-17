package appland.toolwindow.runtimeAnalysis;

import appland.settings.AppMapApplicationSettingsService;
import appland.settings.AppMapSettingsListener;
import appland.toolwindow.runtimeAnalysis.nodes.Node;
import appland.toolwindow.runtimeAnalysis.nodes.RootNode;
import com.intellij.openapi.Disposable;
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

/**
 * Tree model to show a tree of findings in a hierarchy of
 * <pre>
 *  &lt;Project name&gt;
 *    -> Findings
 *       -> &lt;Date Bucket&gt;
 *          -> &lt;Impact domain&gt;
 *             -> &lt;Rule&gt;
 *                -> Finding
 *    -> Failed Tests
 *        -> &lt;AppMap name&gt;
 * </pre>
 */
public class RuntimeAnalysisModel extends BaseTreeModel<Node> implements InvokerSupplier {
    @Getter
    private final @NotNull Invoker invoker = Invoker.forBackgroundPoolWithoutReadAction(this);
    private final @NotNull RootNode root;

    public RuntimeAnalysisModel(@NotNull Project project, @NotNull Disposable parent) {
        this.root = new RootNode(project, this);
        Disposer.register(parent, this);

        // update root node state and the empty panel when the login state changed
        project.getMessageBus().connect(this).subscribe(AppMapSettingsListener.TOPIC, new AppMapSettingsListener() {
            @Override
            public void enableFindingsChanged() {
                structureChanged(null);
            }
        });
    }

    @Override
    public @Nullable RootNode getRoot() {
        if (!AppMapApplicationSettingsService.getInstance().isEnableFindings()) {
            return null;
        }

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
