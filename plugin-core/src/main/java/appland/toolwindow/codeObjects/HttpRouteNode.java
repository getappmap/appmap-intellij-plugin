package appland.toolwindow.codeObjects;

import appland.index.ClassMapItem;
import appland.index.ClassMapItemType;
import com.intellij.ide.util.treeView.NodeDescriptor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

@Getter
class HttpRouteNode extends AbstractClassMapItemNode {
    private final String httpMethodName;
    private final String httpPath;

    protected HttpRouteNode(@NotNull NodeDescriptor parentNode, @NotNull ClassMapItem item) {
        super(parentNode, ClassMapItemType.Route, item, Set.of());

        var displayName = getDisplayName();
        var spaceIndex = displayName.indexOf(' ');
        this.httpMethodName = spaceIndex != -1 ? displayName.substring(0, spaceIndex) : "";
        this.httpPath = spaceIndex != -1 && spaceIndex + 1 < displayName.length() ? displayName.substring(spaceIndex + 1) : "";
    }

    @Override
    protected boolean isNavigable() {
        return true;
    }
}
