package appland.files;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.vfs.AsyncFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileCopyEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent;
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

        return changeTypes.isEmpty()
                ? null
                : new ChangeApplier() {
            @Override
            public void afterVfsChange() {
                notifyAppMapFileChange(changeTypes);
            }
        };
    }

    private static boolean isAppMapFileChange(VFileEvent event) {
        // only treat renames as valid property changes
        if (event instanceof VFilePropertyChangeEvent) {
            if (!((VFilePropertyChangeEvent) event).isRename()) {
                return false;
            }
        }

        return AppMapFiles.isAppMapFileName(PathUtil.getFileName(event.getPath()));
    }

    private void notifyAppMapFileChange(Set<AppMapFileEventType> changeTypes) {
        ApplicationManager.getApplication().getMessageBus()
                .syncPublisher(AppMapFileChangeListener.TOPIC)
                .afterAppMapFileChange(changeTypes);
    }
}
