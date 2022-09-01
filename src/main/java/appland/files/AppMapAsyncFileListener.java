package appland.files;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.vfs.AsyncFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent;
import com.intellij.util.PathUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Async file listener to notify about changes to .appmap.json files.
 */
@SuppressWarnings("UnstableApiUsage")
public class AppMapAsyncFileListener implements AsyncFileListener {
    @Override
    public @Nullable ChangeApplier prepareChange(@NotNull List<? extends @NotNull VFileEvent> events) {
        return events.stream().noneMatch(AppMapAsyncFileListener::isAppMapFileChange)
                ? null
                : new ChangeApplier() {
            @Override
            public void afterVfsChange() {
                notifyAppMapFileChange();
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

    private void notifyAppMapFileChange() {
        ApplicationManager.getApplication().getMessageBus()
                .syncPublisher(AppMapFileChangeListener.TOPIC)
                .afterAppMapFileChange();
    }
}
