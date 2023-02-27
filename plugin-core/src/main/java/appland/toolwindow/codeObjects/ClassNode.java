package appland.toolwindow.codeObjects;

import appland.index.ClassMapItem;
import appland.index.ClassMapItemType;
import com.intellij.ide.util.treeView.NodeDescriptor;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

class ClassNode extends AbstractClassMapItemNode {
    protected ClassNode(@NotNull NodeDescriptor parentNode, @NotNull ClassMapItem item) {
        super(parentNode, ClassMapItemType.Class, item, Set.of(ClassMapItemType.Class, ClassMapItemType.Function));
    }

    @Override
    protected boolean isNavigable() {
        return true;
    }
}
