package appland.utils;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.ui.tree.AsyncTreeModel;
import com.intellij.ui.tree.TreeTestUtil;
import com.intellij.ui.treeStructure.Tree;
import org.jetbrains.annotations.NotNull;

import javax.swing.tree.TreeModel;

public final class ModelTestUtil {
    private ModelTestUtil() {
    }

    public static void assertTreeHierarchy(@NotNull TreeModel model,
                                           @NotNull String expected,
                                           @NotNull Disposable disposable) {
        ApplicationManager.getApplication().invokeAndWait(() -> {
            new TreeTestUtil(new Tree(new AsyncTreeModel(model, disposable)))
                    .expandAll()
                    .expandAll()
                    .expandAll()
                    .expandAll()
                    .expandAll()
                    .assertStructure(expected);
        });
    }
}
