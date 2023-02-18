package appland.toolwindow.codeObjects;

import appland.index.ClassMapItem;
import appland.index.ClassMapItemType;
import com.intellij.ide.util.treeView.NodeDescriptor;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class HttpRouteNode extends AbstractClassMapItemNode {
    protected HttpRouteNode(@NotNull NodeDescriptor parentNode, @NotNull ClassMapItem item) {
        super(parentNode, ClassMapItemType.Route, item, Set.of());
    }

    @Override
    protected boolean isNavigable() {
        return true;
    }
}
