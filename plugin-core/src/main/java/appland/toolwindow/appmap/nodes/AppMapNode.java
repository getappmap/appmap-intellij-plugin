package appland.toolwindow.appmap.nodes;

import appland.Icons;
import appland.index.AppMapMetadata;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.tree.LeafState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

class AppMapNode extends Node {
    private final AppMapMetadata appMap;

    AppMapNode(@NotNull Project project, @Nullable NodeDescriptor parentDescriptor, @NotNull AppMapMetadata appMap) {
        super(project, parentDescriptor);
        this.appMap = appMap;
    }

    @Override
    public @NotNull LeafState getLeafState() {
        return LeafState.ALWAYS;
    }

    @Override
    public List<? extends Node> getChildren() {
        return Collections.emptyList();
    }

    @Override
    public @Nullable VirtualFile getFile() {
        return appMap.getAppMapFile();
    }

    @Override
    protected void update(@NotNull PresentationData presentation) {
        presentation.setPresentableText(appMap.getName());
        presentation.setIcon(Icons.APPMAP_FILE_SMALL);
    }
}
