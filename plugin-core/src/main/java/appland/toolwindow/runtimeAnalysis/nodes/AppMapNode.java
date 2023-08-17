package appland.toolwindow.runtimeAnalysis.nodes;

import appland.Icons;
import appland.files.OpenAppMapFileNavigatable;
import appland.index.AppMapMetadata;
import appland.index.AppMapMetadataService;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.ui.tree.LeafState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * Node representing an AppMap.
 */
class AppMapNode extends Node {
    private final @NotNull VirtualFile appMap;
    private final @Nullable AppMapMetadata appMapMetadata;

    public AppMapNode(@NotNull Project project,
                      @Nullable NodeDescriptor parentDescriptor,
                      @NotNull VirtualFile appMap) {
        super(project, parentDescriptor);
        this.appMap = appMap;

        var metadataService = AppMapMetadataService.getInstance(project);
        this.appMapMetadata = ReadAction.compute(() -> metadataService.getAppMapMetadata(appMap));
    }

    @NotNull
    public String getTitle() {
        return appMapMetadata != null ? appMapMetadata.getName() : appMap.getPresentableName();
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
    protected void update(@NotNull PresentationData presentation) {
        presentation.setIcon(Icons.APPMAP_FILE_SMALL);
        presentation.setPresentableText(getTitle());
    }

    @Override
    public @Nullable Navigatable getNavigatable() {
        return new OpenAppMapFileNavigatable(myProject, appMap, null);
    }
}
