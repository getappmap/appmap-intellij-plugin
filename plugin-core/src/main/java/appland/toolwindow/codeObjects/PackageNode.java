package appland.toolwindow.codeObjects;

import appland.index.ClassMapItem;
import appland.index.ClassMapItemType;
import com.intellij.icons.AllIcons;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class PackageNode extends AbstractClassMapItemNode {
    protected PackageNode(@NotNull NodeDescriptor parentDescriptor, @NotNull ClassMapItem item) {
        super(parentDescriptor, ClassMapItemType.Package, item, Set.of(ClassMapItemType.Package, ClassMapItemType.Class));
    }

    protected PackageNode(@NotNull Project project, @Nullable NodeDescriptor parentDescriptor, @NotNull String nodeId, @NotNull String name) {
        super(project,
                parentDescriptor,
                nodeId,
                name,
                AllIcons.Nodes.Package,
                ClassMapItemType.Package.getId(),
                ClassMapItemType.Package,
                Set.of(ClassMapItemType.Package, ClassMapItemType.Class)
        );
    }

    @Override
    protected boolean isNavigable() {
        return false;
    }
}
