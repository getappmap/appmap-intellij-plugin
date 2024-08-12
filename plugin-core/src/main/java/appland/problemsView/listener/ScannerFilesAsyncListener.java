package appland.problemsView.listener;

import appland.index.AppMapFindingsUtil;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.AsyncFileListener;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.newvfs.events.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.SystemIndependent;

import java.util.HashSet;
import java.util.List;

/**
 * Async file listener to update the problems view of the open projects when appmap-findings.json files
 * are added, removed or otherwise modified.
 */
@SuppressWarnings("UnstableApiUsage")
public class ScannerFilesAsyncListener implements AsyncFileListener {
    @Override
    public @Nullable ChangeApplier prepareChange(@NotNull List<? extends @NotNull VFileEvent> events) {
        var toAdd = new HashSet<@SystemIndependent String>();
        var toRefresh = new HashSet<@SystemIndependent String>();
        var toRemove = new HashSet<@SystemIndependent String>();
        var directories = new HashSet<@SystemIndependent String>();

        for (var event : events) {
            ProgressManager.checkCanceled();

            if (!(event.getFileSystem() instanceof LocalFileSystem)) {
                continue;
            }

            if (isDirectoryEvent(event)) {
                directories.add(event.getPath());
            } else if (event instanceof VFileDeleteEvent) {
                if (AppMapFindingsUtil.isFindingFile(event.getPath())) {
                    toRemove.add(event.getPath());
                }
            } else if (event instanceof VFileMoveEvent) {
                var oldPath = ((VFileMoveEvent) event).getOldPath();
                if (AppMapFindingsUtil.isFindingFile(oldPath)) {
                    toRemove.add(oldPath);
                }

                var newPath = ((VFileMoveEvent) event).getNewPath();
                if (AppMapFindingsUtil.isFindingFile(newPath)) {
                    toAdd.add(newPath);
                }
            } else if (event instanceof VFileCopyEvent) {
                var newFile = ((VFileCopyEvent) event).findCreatedFile();
                if (newFile != null) {
                    toAdd.add(event.getPath());
                }
            } else if (event instanceof VFileCreateEvent) {
                if (AppMapFindingsUtil.isFindingFile(event.getPath())) {
                    toAdd.add(event.getPath());
                }
            } else if (event instanceof VFileContentChangeEvent) {
                if (AppMapFindingsUtil.isFindingFile(event.getPath())) {
                    toRefresh.add(event.getPath());
                }
            } else if (event instanceof VFilePropertyChangeEvent) {
                // file was renamed, still under the same parent directory
                if (((VFilePropertyChangeEvent) event).isRename()) {
                    var oldPath = ((VFilePropertyChangeEvent) event).getOldPath();
                    var newPath = ((VFilePropertyChangeEvent) event).getNewPath();

                    if (AppMapFindingsUtil.isFindingFile(oldPath)) {
                        toRemove.add(oldPath);
                    }

                    if (AppMapFindingsUtil.isFindingFile(newPath)) {
                        toAdd.add(newPath);
                    }
                }
            }
        }

        if (toAdd.isEmpty() && toRemove.isEmpty() && toRefresh.isEmpty() && directories.isEmpty()) {
            return null;
        }

        return new ChangeApplier() {
            @Override
            public void afterVfsChange() {
                for (var project : ProjectManager.getInstance().getOpenProjects()) {
                    if (!project.isDefault() && !project.isDisposed()) {
                        PendingScannerFindingsChangesService
                                .getInstance(project)
                                .queueChanges(directories, toAdd, toRefresh, toRemove);
                    }
                }
            }
        };
    }

    private boolean isDirectoryEvent(VFileEvent event) {
        return event instanceof VFileDeleteEvent && event.getFile().isDirectory()
                || event instanceof VFileCreateEvent && ((VFileCreateEvent) event).isDirectory()
                || event instanceof VFileMoveEvent && event.getFile().isDirectory();
    }
}
