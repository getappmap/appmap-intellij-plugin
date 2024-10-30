package appland.toolwindow.appmap.nodes;

import appland.index.AppMapMetadata;
import appland.index.AppMapMetadataService;
import appland.toolwindow.appmap.AppMapModel;
import appland.utils.AppMapProjectUtil;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.tree.LeafState;
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread;
import com.intellij.util.concurrency.annotations.RequiresReadLock;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class RootNode extends Node {
    private final @NotNull AppMapModel model;
    private final AtomicBoolean needAppMapRefresh = new AtomicBoolean(true);
    private final AtomicReference<List<AppMapMetadata>> cachedAppMaps = new AtomicReference<>();

    public RootNode(@NotNull Project project, @NotNull AppMapModel model) {
        super(project, null);
        this.model = model;
    }

    @Override
    public @NotNull LeafState getLeafState() {
        return LeafState.NEVER;
    }

    @Override
    protected void update(@NotNull PresentationData presentation) {
        // the root node isn't visible
        presentation.setPresentableText("AppMaps");
    }

    public void queueAppMapRefresh() {
        needAppMapRefresh.set(true);
    }

    @Override
    public List<? extends Node> getChildren() {
        var appMaps = getCachedAppMaps();
        var byProjectName = ReadAction.compute(() -> appMaps.stream()
                .filter(appMap -> appMap.getAppMapFile() != null)
                .collect(Collectors.groupingBy(this::getAppMapProjectName)));

        return byProjectName.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> (Node) new ProjectNode(myProject, this, entry.getKey(), entry.getValue()))
                .collect(Collectors.toCollection(LinkedList::new));
    }

    @Override
    public @NotNull List<VirtualFile> getFiles() {
        var appMaps = getCachedAppMaps();
        if (appMaps.isEmpty()) {
            return Collections.emptyList();
        }

        return appMaps.stream()
                .map(AppMapMetadata::getAppMapFile)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @RequiresReadLock
    private @NotNull String getAppMapProjectName(@NotNull AppMapMetadata appMapMetadata) {
        assert appMapMetadata.getAppMapFile() != null;
        return AppMapProjectUtil.getAppMapProjectName(myProject, appMapMetadata.getAppMapFile());
    }

    /**
     * @return Available AppMaps, fetched in a {@code com.intellij.openapi.application.ReadAction}.
     */
    @RequiresBackgroundThread
    private @NotNull List<AppMapMetadata> getCachedAppMaps() {
        // If we're on the EDT, we're unable to load the AppMaps because it involves slow operations. Also, we're unable
        // to launch a Task.WithResult because it's causing an exception about nested access to DataContext.
        // If we're on the EDT and if the AppMaps have not been cached yet, we're returning an empty list.
        if (needAppMapRefresh.get() && !ApplicationManager.getApplication().isDispatchThread()) {
            cachedAppMaps.set(loadAppMaps());
            needAppMapRefresh.set(false);
        }

        // a null value is possible for concurrent calls to this method
        var result = cachedAppMaps.get();
        return result == null ? Collections.emptyList() : result;
    }

    private @NotNull List<AppMapMetadata> loadAppMaps() {
        return DumbService.getInstance(myProject).runReadActionInSmartMode(() -> {
            return AppMapMetadataService.getInstance(myProject).findAppMaps(model.getNameFilter());
        });
    }
}
