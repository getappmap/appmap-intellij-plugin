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
    private final String groupName;
    private final List<AppMapMetadata> appMaps;

    AppMapGroupNode(@NotNull Project project,
                    @NotNull NodeDescriptor parentDescriptor,
                    @NotNull String groupName,
                    @NotNull List<AppMapMetadata> appMaps) {
        super(project, parentDescriptor);
        this.groupName = groupName;
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
        return appMaps.stream()
                .sorted(Comparator.comparing(AppMapMetadata::getName))
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
