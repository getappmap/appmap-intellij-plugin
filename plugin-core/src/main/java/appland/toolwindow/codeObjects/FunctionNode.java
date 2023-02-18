package appland.toolwindow.codeObjects;

import appland.index.ClassMapItem;
import appland.index.ClassMapItemType;
import com.intellij.ide.util.treeView.NodeDescriptor;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class FunctionNode extends AbstractClassMapItemNode {
    protected FunctionNode(@NotNull NodeDescriptor parentDescriptor, @NotNull ClassMapItem item) {
        super(parentDescriptor, ClassMapItemType.Function, item, Set.of());
    }

    @Override
    protected boolean isNavigable() {
        return true;
    }
}
