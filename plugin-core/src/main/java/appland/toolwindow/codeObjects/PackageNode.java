package appland.toolwindow.codeObjects;

import appland.index.ClassMapItem;
import appland.index.ClassMapItemType;
import com.intellij.ide.util.treeView.NodeDescriptor;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

class PackageNode extends AbstractClassMapItemNode {
    protected PackageNode(@NotNull NodeDescriptor parentDescriptor, @NotNull ClassMapItem item) {
        super(parentDescriptor, ClassMapItemType.Package, item, Set.of(ClassMapItemType.Package, ClassMapItemType.Class));
    }

    @Override
    protected boolean isNavigable() {
        return false;
    }
}
