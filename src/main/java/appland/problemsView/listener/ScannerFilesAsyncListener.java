package appland.problemsView.listener;

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.AsyncFileListener;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.events.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;

import static appland.problemsView.FindingsManager.getInstance;
import static appland.problemsView.FindingsManager.isFindingFile;

/**
 * Async file listener to update the problems view of the open projects when appmap-findings.json files
 * are added, removed or otherwise modified.
 */
@SuppressWarnings("UnstableApiUsage")
public class ScannerFilesAsyncListener implements AsyncFileListener {
    @Override
    public @Nullable ChangeApplier prepareChange(@NotNull List<? extends @NotNull VFileEvent> events) {
        var toAdd = new HashSet<VirtualFile>();
        var toRefresh = new HashSet<VirtualFile>();
        var toRemove = new HashSet<String>();

        for (VFileEvent event : events) {
            ProgressManager.checkCanceled();

            if (event instanceof VFileDeleteEvent) {
                if (isFindingFile(event.getPath())) {
                    toRemove.add(event.getPath());
                }
            } else if (event instanceof VFileMoveEvent) {
                var oldPath = ((VFileMoveEvent) event).getOldPath();
                if (isFindingFile(oldPath)) {
                    toRemove.add(oldPath);
                }

                var newPath = ((VFileMoveEvent) event).getNewPath();
                if (isFindingFile(newPath)) {
                    toAdd.add(event.getFile());
                }
            } else if (event instanceof VFileCopyEvent) {
                var newFile = ((VFileCopyEvent) event).findCreatedFile();
                if (newFile != null) {
                    toAdd.add(newFile);
                }
            } else if (event instanceof VFileCreateEvent) {
                if (isFindingFile(event.getPath())) {
                    toAdd.add(event.getFile());
                }
            } else if (event instanceof VFileContentChangeEvent) {
                if (isFindingFile(event.getPath())) {
                    toRefresh.add(event.getFile());
                }
            } else if (event instanceof VFilePropertyChangeEvent) {
                // file was renamed, still under the same parent directory
                if (((VFilePropertyChangeEvent) event).isRename()) {
                    var oldPath = ((VFilePropertyChangeEvent) event).getOldPath();
                    var newPath = ((VFilePropertyChangeEvent) event).getNewPath();

                    if (isFindingFile(oldPath)) {
                        toRemove.add(oldPath);
                    }

                    if (isFindingFile(newPath)) {
                        toAdd.add(event.getFile());
                    }
                }
            }
        }

        if (toAdd.isEmpty() && toRemove.isEmpty()) {
            return null;
        }

        return new ChangeApplier() {
            @Override
            public void afterVfsChange() {
                for (var project : ProjectManager.getInstance().getOpenProjects()) {
                    if (!project.isDefault()) {
                        var manager = getInstance(project);

                        for (var deletedFile : toRemove) {
                            manager.removeFindingsFile(deletedFile);
                        }

                        for (var file : toAdd) {
                            manager.addFindingsFile(file);
                        }

                        for (var file : toRefresh) {
                            manager.reloadFindingsFile(file);
                        }
                    }
                }
            }
        };
    }
}
