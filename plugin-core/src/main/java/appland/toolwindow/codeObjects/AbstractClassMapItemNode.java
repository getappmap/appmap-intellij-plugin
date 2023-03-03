package appland.toolwindow.codeObjects;

import appland.index.ClassMapItem;
import appland.index.ClassMapItemType;
import appland.index.ClassMapTypeIndex;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.pom.Navigatable;
import com.intellij.ui.tree.LeafState;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.*;

abstract class AbstractClassMapItemNode extends Node {
    @Getter
    private final @NotNull String nodeId;
    @Getter
    private final @NotNull String displayName;
    private final @Nullable String location;
    private final @NotNull Set<ClassMapItemType> supportedChildrenTypes;
    private final @Nullable Icon nodeIcon;
    @Getter
    private final int weight;
    @Getter
    private final @NotNull ClassMapItemType nodeType;

    protected AbstractClassMapItemNode(@NotNull NodeDescriptor<?> parentNode,
                                       @NotNull ClassMapItemType itemType,
                                       @NotNull ClassMapItem item,
                                       @NotNull Set<ClassMapItemType> supportedChildrenTypes) {
        this(parentNode.getProject(),
                parentNode,
                item.getId(),
                item.getName(),
                item.getLocation(),
                itemType.getIcon(),
                itemType.getId(),
                itemType,
                supportedChildrenTypes);
    }

    protected AbstractClassMapItemNode(@NotNull Project project,
                                       @Nullable NodeDescriptor parentDescriptor,
                                       @NotNull String nodeId,
                                       @NotNull String displayName,
                                       @Nullable String location,
                                       @Nullable Icon nodeIcon,
                                       int weight,
                                       @NotNull ClassMapItemType nodeType,
                                       @NotNull Set<ClassMapItemType> supportedChildrenTypes) {
        super(project, parentDescriptor);
        this.displayName = displayName;
        this.nodeId = nodeId;
        this.location = location;
        this.supportedChildrenTypes = supportedChildrenTypes;
        this.nodeIcon = nodeIcon;
        this.weight = weight;
        this.nodeType = nodeType;
    }

    @Override
    public List<? extends Node> getChildren() {
        if (supportedChildrenTypes.isEmpty()) {
            return Collections.emptyList();
        }

        var children = new ArrayList<Node>();

        // multiple ClassMap files may contain the same package, we only want to insert it once
        var visitedNodeIds = new HashSet<String>();

        for (var type : supportedChildrenTypes) {
            ClassMapTypeIndex.processItems(myProject, type, (file, classMapItems) -> {
                for (var item : classMapItems) {
                    var id = item.getId();
                    if (!visitedNodeIds.contains(id) && isValidChildNode(item)) {
                        children.add(createChildNode(type, item));
                        visitedNodeIds.add(id);
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
        return supportedChildrenTypes.isEmpty() ? LeafState.ALWAYS : LeafState.ASYNC;
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
        return new ClassMapItemNavigatable(myProject, nodeType, nodeId, location);
    }
}
