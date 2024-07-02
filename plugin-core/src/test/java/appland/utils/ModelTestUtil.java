package appland.utils;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.ui.tree.AsyncTreeModel;
import com.intellij.ui.tree.TreeTestUtil;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.EDT;
import org.jetbrains.annotations.NotNull;

import javax.swing.tree.TreeModel;

public final class ModelTestUtil {
    private ModelTestUtil() {
    }

    public static void assertTreeHierarchy(@NotNull TreeModel model,
                                           @NotNull String expected,
                                           @NotNull Disposable disposable) {
        if (EDT.isCurrentThreadEdt()) {
            assertTreeHierarchyOnEDT(model, expected, disposable);
        } else {
            ApplicationManager.getApplication().invokeAndWait(() -> {
                assertTreeHierarchyOnEDT(model, expected, disposable);
            });
        }
    }

    private static void assertTreeHierarchyOnEDT(@NotNull TreeModel model,
                                                 @NotNull String expected,
                                                 @NotNull Disposable disposable) {
        new TreeTestUtil(new Tree(new AsyncTreeModel(model, disposable)))
                .expandAll()
                .expandAll()
                .expandAll()
                .expandAll()
                .expandAll()
                .assertStructure(expected);
    }
}
