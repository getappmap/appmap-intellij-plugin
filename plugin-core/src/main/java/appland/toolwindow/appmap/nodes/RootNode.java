package appland.toolwindow.appmap.nodes;

import appland.index.AppMapMetadata;
import appland.index.AppMapMetadataService;
import appland.toolwindow.appmap.AppMapModel;
import appland.utils.AppMapProjectUtil;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.ui.tree.LeafState;
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
        var byProjectName = ReadAction.compute(() -> {
            return getCachedAppMaps().stream()
                    .filter(appMap -> appMap.getAppMapFile() != null)
                    .collect(Collectors.groupingBy(this::getAppMapProjectName));
        });

        return byProjectName.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> (Node) new ProjectNode(myProject, this, entry.getKey(), entry.getValue()))
                .collect(Collectors.toCollection(LinkedList::new));
    }

    private @NotNull String getAppMapProjectName(@NotNull AppMapMetadata appMapMetadata) {
        assert appMapMetadata.getAppMapFile() != null;
        return AppMapProjectUtil.getAppMapProjectName(myProject, appMapMetadata.getAppMapFile());
    }

    @RequiresBackgroundThread
    private List<AppMapMetadata> getCachedAppMaps() {
        if (needAppMapRefresh.compareAndSet(true, false)) {
            cachedAppMaps.set(loadAppMaps());
        }
        return cachedAppMaps.get();
    }

    private List<AppMapMetadata> loadAppMaps() {
        return DumbService.getInstance(myProject).runReadActionInSmartMode(() -> {
            return AppMapMetadataService.getInstance(myProject).findAppMaps(model.getNameFilter());
        });
    }
}
