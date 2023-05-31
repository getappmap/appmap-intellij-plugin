package appland.toolwindow;

import appland.Icons;
import appland.index.AppMapMetadata;
import appland.index.AppMapMetadataService;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.tree.BaseTreeModel;
import com.intellij.util.ObjectUtils;
import com.intellij.util.concurrency.Invoker;
import com.intellij.util.concurrency.InvokerSupplier;
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class AppMapModel extends BaseTreeModel<Object> implements InvokerSupplier {
    private final Object root = ObjectUtils.sentinel("services root");
    private final Invoker invoker = Invoker.forBackgroundPoolWithoutReadAction(this);
    private final AtomicBoolean needAppMapRefresh = new AtomicBoolean(true);
    private final AtomicReference<List<AppMapMetadata>> cachedAppMaps = new AtomicReference<>();
    private final Project project;
    private final AtomicReference<String> nameFilter = new AtomicReference<>();

    public AppMapModel(Project project) {
        this.project = project;
    }

    public void setNameFilter(@Nullable String name) {
        var old = nameFilter.getAndSet(name);
        if (!Objects.equals(old, name)) {
            refresh();
        }
    }

    public void refresh() {
        needAppMapRefresh.set(true);
        treeStructureChanged(new TreePath(root), null, null);
    }

    @Override
    public @NotNull Invoker getInvoker() {
        return invoker;
    }

    @Override
    public Object getRoot() {
        return root;
    }

    @Override
    public boolean isLeaf(Object object) {
        return object != root;
    }

    @Override
    public List<?> getChildren(Object parent) {
        if (parent != root) {
            return Collections.emptyList();
        }

        if (needAppMapRefresh.compareAndSet(true, false)) {
            cachedAppMaps.set(calculateAppMaps());
        }

        return cachedAppMaps.get();
    }

    @RequiresBackgroundThread
    private List<AppMapMetadata> calculateAppMaps() {
        return DumbService.getInstance(project).runReadActionInSmartMode(() -> {
            return AppMapMetadataService.getInstance(project).findAppMaps(nameFilter.get());
        });
    }

    static class TreeCellRenderer extends ColoredTreeCellRenderer {
        @Override
        public void customizeCellRenderer(@NotNull JTree tree,
                                          Object value,
                                          boolean selected,
                                          boolean expanded,
                                          boolean leaf,
                                          int row,
                                          boolean hasFocus) {
            if (!(value instanceof AppMapMetadata)) {
                return;
            }

            var data = (AppMapMetadata) value;
            setIcon(Icons.APPMAP_FILE_SMALL);
            append(data.getName());
            append(" " + data.getFilename(), SimpleTextAttributes.GRAYED_ATTRIBUTES);
        }
    }
}
