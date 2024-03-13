package appland.toolwindow.appmap.nodes;

import appland.index.AppMapMetadata;
import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.tree.LeafState;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

class AppMapGroupNode extends Node {
    private final @NotNull String groupName;
    private final boolean sortItemsByModificationDate;
    private final @NotNull List<AppMapMetadata> appMaps;
    private final Comparator<AppMapMetadata> nameComparator = Comparator.comparing(AppMapMetadata::getName);
    private final Comparator<AppMapMetadata> modificationDateComparator = Comparator.comparingLong(AppMapMetadata::getModificationTimestamp).reversed();

    AppMapGroupNode(@NotNull Project project,
                    @NotNull NodeDescriptor parentDescriptor,
                    @NotNull String groupName,
                    boolean sortItemsByModificationDate,
                    @NotNull List<AppMapMetadata> appMaps) {
        super(project, parentDescriptor);
        this.groupName = groupName;
        this.sortItemsByModificationDate = sortItemsByModificationDate;
        this.appMaps = appMaps;
    }

    @Override
    public @NotNull LeafState getLeafState() {
        return LeafState.NEVER;
    }

    @Override
    protected void update(@NotNull PresentationData presentation) {
        presentation.setPresentableText(groupName);
        presentation.setIcon(AllIcons.Nodes.Folder);
    }

    @Override
    public List<? extends Node> getChildren() {
        var comparator = sortItemsByModificationDate
                ? modificationDateComparator.thenComparing(nameComparator)
                : nameComparator;

        return appMaps.stream()
                .sorted(comparator)
                .map(appMap -> new AppMapNode(myProject, this, appMap))
                .collect(Collectors.toList());
    }

    @Override
    public @NotNull List<VirtualFile> getFiles() {
        return appMaps.stream()
                .map(AppMapMetadata::getAppMapFile)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
