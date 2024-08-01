package appland.files;

import com.intellij.openapi.vfs.AsyncFileListener;
import com.intellij.openapi.vfs.newvfs.events.*;
import com.intellij.util.PathUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Async file listener to notify about changes to .appmap.json files.
 */
@SuppressWarnings("UnstableApiUsage")
public class AppMapAsyncFileListener implements AsyncFileListener {
    @Override
    public @Nullable ChangeApplier prepareChange(@NotNull List<? extends @NotNull VFileEvent> events) {
        var changeTypes = events.stream().filter(event -> AppMapAsyncFileListener.isAppMapFileChange(event)).map(event -> {
            if (event instanceof VFileCreateEvent || event instanceof VFileCopyEvent){
                return AppMapFileEventType.Create;
            }
            if (event instanceof VFileContentChangeEvent) {
                return AppMapFileEventType.Modify;
            }
            if (event instanceof VFileDeleteEvent) {
                return AppMapFileEventType.Delete;
            }
            if (event instanceof VFilePropertyChangeEvent && ((VFilePropertyChangeEvent) event).isRename()) {
                return AppMapFileEventType.Rename;
            }

            return AppMapFileEventType.Other;
        }).collect(Collectors.toSet());
        
        return changeTypes.isEmpty() ? null : new AppMapChangeApplier(changeTypes);
    }

    private static boolean isAppMapFileChange(@NotNull VFileEvent event) {
        // only treat renames as valid property changes
        if (event instanceof VFilePropertyChangeEvent) {
            if (!((VFilePropertyChangeEvent) event).isRename()) {
                return false;
            }
        }

        return AppMapFiles.isAppMapFileName(PathUtil.getFileName(event.getPath()));
    }

    private static class AppMapChangeApplier implements ChangeApplier {
        private final Set<AppMapFileEventType> changeTypes;

        public AppMapChangeApplier(Set<AppMapFileEventType> changeTypes) {
            this.changeTypes = changeTypes;
        }

        @Override
        public void afterVfsChange() {
            AppMapFileChangeListener.sendNotification(changeTypes, false);
        }
    }
}
