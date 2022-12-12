package appland.problemsView.listener;

import appland.index.AppMapFindingsUtil;
import appland.problemsView.FindingsManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.vfs.AsyncFileListener;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.events.*;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.concurrency.annotations.RequiresReadLock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * Async file listener to update the problems view of the open projects when appmap-findings.json files
 * are added, removed or otherwise modified.
 */
@SuppressWarnings("UnstableApiUsage")
public class ScannerFilesAsyncListener implements AsyncFileListener {
    @TestOnly
    private static volatile boolean TEST_ENABLED = true;

    @TestOnly
    public static <T> T disableForTests(@NotNull ThrowableComputable<T, Exception> runnable) throws Exception {
        TEST_ENABLED = false;
        try {
            return runnable.compute();
        } finally {
            TEST_ENABLED = true;
        }
    }

    private static final @NotNull ExecutorService executor = AppExecutorUtil.createBoundedApplicationPoolExecutor("AppMap file changes", 2);

    @Override
    public @Nullable ChangeApplier prepareChange(@NotNull List<? extends @NotNull VFileEvent> events) {
        if (!TEST_ENABLED && ApplicationManager.getApplication().isUnitTestMode()) {
            return null;
        }

        var toAdd = new HashSet<Supplier<VirtualFile>>();
        var toRefresh = new HashSet<VirtualFile>();
        var toRemove = new HashSet<String>();
        var directories = new HashSet<String>();

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
                    toAdd.add(event::getFile);
                }
            } else if (event instanceof VFileCopyEvent) {
                var newFile = ((VFileCopyEvent) event).findCreatedFile();
                if (newFile != null) {
                    toAdd.add(() -> newFile);
                }
            } else if (event instanceof VFileCreateEvent) {
                if (AppMapFindingsUtil.isFindingFile(event.getPath())) {
                    toAdd.add(event::getFile);
                }
            } else if (event instanceof VFileContentChangeEvent) {
                if (AppMapFindingsUtil.isFindingFile(event.getPath())) {
                    toRefresh.add(event.getFile());
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
                        toAdd.add(event::getFile);
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
                    if (project.isDefault() || project.isDisposed()) {
                        continue;
                    }

                    // we need to run in smart mode because the findings manager uses the index to find relative files
                    ReadAction.nonBlocking(() -> processChangesAsync(project, toAdd, toRemove, toRefresh, directories))
                            .inSmartMode(project)
                            .submit(executor);
                }
            }
        };
    }

    @RequiresReadLock
    private void processChangesAsync(@NotNull Project project,
                                     Set<Supplier<VirtualFile>> toAdd,
                                     Set<String> toRemove,
                                     Set<VirtualFile> toRefresh,
                                     Set<String> directories) {
        if (project.isDisposed()) {
            return;
        }

        var manager = FindingsManager.getInstance(project);

        // if there's at least one modified directory with finding files, then we do a complete reload
        // instead of attempting to find the best diff
        for (var path : directories) {
            var directory = LocalFileSystem.getInstance().findFileByPath(path);
            // either a deleted directory, which may have contained AppMaps
            // or a new/modified directory, which contains AppMaps
            if (directory == null || isDirectoryWithFindingFiles(directory)) {
                manager.reloadAsync();
                return;
            }
        }

        for (var deletedFile : toRemove) {
            if (deletedFile != null) {
                manager.removeFindingsFile(deletedFile);
            }
        }

        for (var supplier : toAdd) {
            var file = supplier.get();
            if (file != null) {
                manager.addFindingsFile(file);
            }
        }

        for (var file : toRefresh) {
            if (file != null) {
                manager.reloadFindingsFile(file);
            }
        }
    }

    @RequiresReadLock
    private boolean isDirectoryWithFindingFiles(@NotNull VirtualFile directory) {
        var hasFindings = new AtomicBoolean(false);
        VfsUtilCore.iterateChildrenRecursively(directory,
                fileOrDir -> fileOrDir.isDirectory() || AppMapFindingsUtil.isFindingFile(fileOrDir),
                fileOrDir -> {
                    if (fileOrDir.isDirectory()) {
                        return true;
                    }
                    var isFindingsFile = AppMapFindingsUtil.isFindingFile(fileOrDir);
                    hasFindings.set(isFindingsFile);
                    return !isFindingsFile;
                }
        );
        return hasFindings.get();
    }

    private boolean isDirectoryEvent(VFileEvent event) {
        return event instanceof VFileDeleteEvent && event.getFile() != null && event.getFile().isDirectory()
                || event instanceof VFileCreateEvent && ((VFileCreateEvent) event).isDirectory()
                || event instanceof VFileMoveEvent && event.getFile() != null && event.getFile().isDirectory();
    }
}
