package appland.toolwindow.runtimeAnalysis;

import appland.toolwindow.runtimeAnalysis.nodes.Node;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.pom.Navigatable;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.TreeSpeedSearch;
import com.intellij.ui.tree.AsyncTreeModel;
import com.intellij.ui.tree.RestoreSelectionListener;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.EditSourceOnDoubleClickHandler;
import com.intellij.util.EditSourceOnEnterKeyHandler;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;

import static javax.swing.tree.TreeSelectionModel.SINGLE_TREE_SELECTION;

public class RuntimeAnalysisPanel extends JPanel implements Disposable, DataProvider {
    private final Tree tree;
    private final Project project;

    public RuntimeAnalysisPanel(@NotNull Project project, @NotNull Disposable parent) {
        super(new BorderLayout());
        Disposer.register(parent, this);

        this.project = project;
        this.tree = setupTree(project, this);

        add(ScrollPaneFactory.createScrollPane(tree, true));
    }

    @Override
    public void dispose() {
    }

    @Override
    public @Nullable Object getData(@NotNull @NonNls String dataId) {
        if (CommonDataKeys.PROJECT.is(dataId)) {
            return project;
        }

        Node node = getSelectedNode();
        if (node != null) {
            if (PlatformCoreDataKeys.SELECTED_ITEM.is(dataId)) {
                return node;
            }

            if (CommonDataKeys.NAVIGATABLE.is(dataId)) {
                return node.getNavigatable();
            }

            if (CommonDataKeys.NAVIGATABLE_ARRAY.is(dataId)) {
                Navigatable navigatable = node.getNavigatable();
                return navigatable == null ? null : new Navigatable[]{navigatable};
            }
        }

        return null;
    }

    private @Nullable Node getSelectedNode() {
        return getNode(tree.getSelectionPath());
    }

    private static @Nullable Node getNode(@Nullable TreePath path) {
        return TreeUtil.getLastUserObject(Node.class, path);
    }

    private static @NotNull Tree setupTree(@NotNull Project project, @NotNull Disposable parent) {
        var tree = new Tree(new AsyncTreeModel(new RuntimeAnalysisModel(project, parent), parent));
        tree.setRootVisible(false);
        tree.setVisibleRowCount(8);
        tree.getSelectionModel().setSelectionMode(SINGLE_TREE_SELECTION);
        tree.addTreeSelectionListener(new RestoreSelectionListener());
        new TreeSpeedSearch(tree);
        EditSourceOnDoubleClickHandler.install(tree);
        EditSourceOnEnterKeyHandler.install(tree);

        return tree;
    }
}
