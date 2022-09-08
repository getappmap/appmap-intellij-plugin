package appland.files;

import com.intellij.openapi.vfs.AsyncFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent;
import com.intellij.util.PathUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Async file listener to notify about changes to .appmap.json files.
 * We don't react to changes from a file system refresh.
 * {@link  VirtualFileManagerLister} is taking care of refresh events.
 */
@SuppressWarnings("UnstableApiUsage")
public class AppMapAsyncFileListener implements AsyncFileListener {
    @Override
    public @Nullable ChangeApplier prepareChange(@NotNull List<? extends @NotNull VFileEvent> events) {
        var hasEvents = events.stream().anyMatch(AppMapAsyncFileListener::isAppMapFileChange);
        return hasEvents ? new AppMapChangeApplier() : null;
    }

    private static boolean isAppMapFileChange(@NotNull VFileEvent event) {
        if (event.isFromRefresh()) {
            return false;
        }

        // only treat renames as valid property changes
        if (event instanceof VFilePropertyChangeEvent) {
            if (!((VFilePropertyChangeEvent) event).isRename()) {
                return false;
            }
        }

        return AppMapFiles.isAppMapFileName(PathUtil.getFileName(event.getPath()));
    }

    private static class AppMapChangeApplier implements ChangeApplier {
        @Override
        public void afterVfsChange() {
            AppMapFileChangeListener.sendNotification();
        }
    }
}
