package appland.toolwindow.codeObjects;

import appland.index.ClassMapItem;
import appland.index.ClassMapItemType;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

class CodeObjectTopLevelNode extends AbstractClassMapItemNode {
    CodeObjectTopLevelNode(@NotNull Node parentNode,
                           @NotNull ClassMapItemType type,
                           @NotNull String id,
                           @NotNull String label,
                           @NotNull Set<ClassMapItemType> supportedChildrenTypes) {
        super(parentNode.getProject(), parentNode, id, label, type.getIcon(), type.getId(), type, supportedChildrenTypes);
    }

    @Override
    protected boolean isValidChildNode(@NotNull ClassMapItem item) {
        var id = getNodeId();
        return id.isEmpty() || id.equals(item.getParentId());
    }

    @Override
    protected boolean isNavigable() {
        return false;
    }
}
