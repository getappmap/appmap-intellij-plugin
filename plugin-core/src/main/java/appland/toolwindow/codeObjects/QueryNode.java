package appland.toolwindow.codeObjects;

import appland.index.ClassMapItem;
import appland.index.ClassMapItemType;
import com.intellij.ide.util.treeView.NodeDescriptor;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

class QueryNode extends AbstractClassMapItemNode {
    protected QueryNode(@NotNull NodeDescriptor parentNode, @NotNull ClassMapItem item) {
        super(parentNode, ClassMapItemType.Query, item, Set.of());
    }

    @Override
    protected boolean isNavigable() {
        return true;
    }
}
