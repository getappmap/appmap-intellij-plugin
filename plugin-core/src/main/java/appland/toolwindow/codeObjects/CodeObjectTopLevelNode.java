package appland.toolwindow.codeObjects;

import appland.index.ClassMapItem;
import appland.index.ClassMapItemType;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

class CodeObjectTopLevelNode extends AbstractClassMapItemNode {
    CodeObjectTopLevelNode(@NotNull Node parentNode,
                           @NotNull ClassMapItemType type,
                           @NotNull String label,
                           @NotNull Set<ClassMapItemType> supportedChildrenTypes) {
        super(parentNode.getProject(), parentNode, "", label, type.getIcon(), type.getId(), type, supportedChildrenTypes);
    }

    @Override
    protected boolean isValidChildNode(@NotNull ClassMapItem item) {
        // we're assuming that "supportedChildrenTypes" is correctly used
        return true;
    }

    @Override
    protected boolean isNavigable() {
        return false;
    }
}
