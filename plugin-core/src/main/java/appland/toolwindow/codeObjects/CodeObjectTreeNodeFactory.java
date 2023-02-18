package appland.toolwindow.codeObjects;

import appland.index.ClassMapItem;
import appland.index.ClassMapItemType;
import org.jetbrains.annotations.NotNull;

class CodeObjectTreeNodeFactory {
    static @NotNull Node createCodeObjectTreeNode(@NotNull Node parent, @NotNull ClassMapItemType type, @NotNull ClassMapItem item) {
        switch (type) {
            case Package:
                return new PackageNode(parent, item);
            case Route:
                return new HttpRouteNode(parent, item);
            case Query:
                return new QueryNode(parent, item);
            case Class:
                return new ClassNode(parent, item);
            case Function:
                return new FunctionNode(parent, item);
            default:
                throw new IllegalStateException("unsupported node type: " + type);
        }
    }
}
