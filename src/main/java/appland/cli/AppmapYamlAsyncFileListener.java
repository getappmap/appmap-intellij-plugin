package appland.cli;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.AsyncFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.util.PathUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Listens for changes to appmap.yml files and performs a refresh of CLI processes for compatible content roots.
 */
public class AppmapYamlAsyncFileListener implements AsyncFileListener {
    @Override
    public @Nullable ChangeApplier prepareChange(@NotNull List<? extends @NotNull VFileEvent> events) {
        var matchingEvents = events
                .stream()
                .filter(this::isAppMapYamlEvent)
                .collect(Collectors.toList());

        if (matchingEvents.isEmpty()) {
            return null;
        }

        return new ChangeApplier() {
            @Override
            public void afterVfsChange() {
                AppLandCommandLineService.getInstance().refreshForOpenProjects();
            }
        };
    }

    /**
     * We're listing for any change to an appmap.yml file to start when it's created
     * and to stop services when it became unavailable, e.g. after a rename, move, or removal.
     */
    private boolean isAppMapYamlEvent(@NotNull VFileEvent event) {
        return FileUtil.fileNameEquals(PathUtil.getFileName(event.getPath()), "appmap.yml");
    }
}
