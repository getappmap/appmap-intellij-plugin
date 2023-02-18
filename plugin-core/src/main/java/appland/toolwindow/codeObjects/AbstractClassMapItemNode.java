package appland.toolwindow.codeObjects;

import appland.index.ClassMapItem;
import appland.index.ClassMapItemType;
import appland.index.ClassMapTypeIndex;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.pom.Navigatable;
import com.intellij.ui.tree.LeafState;
import com.intellij.util.containers.SortedList;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

abstract class AbstractClassMapItemNode extends Node {
    private final @NotNull String nodeId;
    @Getter
    private final @NotNull String displayName;
    private final @NotNull Set<ClassMapItemType> supportedChildrenTypes;
    private final @Nullable Icon nodeIcon;
    @Getter
    private final int priority;
    private final @NotNull ClassMapItemType nodeType;

    protected AbstractClassMapItemNode(@NotNull NodeDescriptor<?> parentNode,
                                       @NotNull ClassMapItemType itemType,
                                       @NotNull ClassMapItem item,
                                       @NotNull Set<ClassMapItemType> supportedChildrenTypes) {
        this(parentNode.getProject(), parentNode, item.getId(), item.getName(), itemType.getIcon(), itemType.getId(), itemType, supportedChildrenTypes);
    }

    protected AbstractClassMapItemNode(@NotNull Project project,
                                       @Nullable NodeDescriptor parentDescriptor,
                                       @NotNull String nodeId,
                                       @NotNull String displayName,
                                       @Nullable Icon nodeIcon,
                                       int priority,
                                       @NotNull ClassMapItemType nodeType,
                                       @NotNull Set<ClassMapItemType> supportedChildrenTypes) {
        super(project, parentDescriptor);
        this.displayName = displayName;
        this.nodeId = nodeId;
        this.supportedChildrenTypes = supportedChildrenTypes;
        this.nodeIcon = nodeIcon;
        this.priority = priority;
        this.nodeType = nodeType;
    }

    @Override
    public List<? extends Node> getChildren() {
        if (supportedChildrenTypes.isEmpty()) {
            return Collections.emptyList();
        }

        var children = new SortedList<Node>((a, b) -> {
            if (a.getPriority() != b.getPriority()) {
                return a.getPriority() - b.getPriority();
            }
            return a.getDisplayName().compareTo(b.getDisplayName());
        });
        // multiple ClassMap files may contain the same package, we only want to insert it once
        var visitedPackageIds = new HashSet<String>();

        for (var type : supportedChildrenTypes) {
            ClassMapTypeIndex.processItems(myProject, type, (file, classMapItems) -> {
                for (var item : classMapItems) {
                    var id = item.getId();
                    if (!visitedPackageIds.contains(id) && isValidChildNode(item)) {
                        children.add(createChildNode(type, item));
                        visitedPackageIds.add(id);
                    }
                }
                return true;
            });
        }

        return children;
    }

    @NotNull
    protected final Node createChildNode(@NotNull ClassMapItemType type, @NotNull ClassMapItem item) {
        return CodeObjectTreeNodeFactory.createCodeObjectTreeNode(this, type, item);
    }

    @Override
    protected void update(@NotNull PresentationData presentation) {
        presentation.setPresentableText(displayName);
        presentation.setIcon(nodeIcon);
    }

    @Override
    public @NotNull LeafState getLeafState() {
        return LeafState.ASYNC;
    }

    protected boolean isValidChildNode(@NotNull ClassMapItem item) {
        return nodeId.equals(item.getParentId());
    }

    abstract protected boolean isNavigable();

    @Override
    public @Nullable Navigatable getNavigatable() {
        if (!isNavigable()) {
            return null;
        }

        return new ClassMapItemNavigatable(myProject, nodeType, nodeId);
    }
}
