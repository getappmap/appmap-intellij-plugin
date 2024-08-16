package appland.problemsView.listener;

import appland.index.AppMapFindingsUtil;
import appland.problemsView.FindingsManager;
import com.google.common.collect.Sets;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Alarm;
import com.intellij.util.SingleAlarm;
import com.intellij.util.concurrency.annotations.RequiresReadLock;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.SystemIndependent;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service to handle the queue of collected finding file changes.
 * Changes to findings must be debounced and only be applied when no other file changes are in progress.
 */
@Service(Service.Level.PROJECT)
public final class PendingScannerFindingsChangesService implements Disposable {
    public static @NotNull PendingScannerFindingsChangesService getInstance(@NotNull Project project) {
        return project.getService(PendingScannerFindingsChangesService.class);
    }

    private final Project project;
    private final SingleAlarm alarm = new SingleAlarm(this::processChanges, 5_000, Alarm.ThreadToUse.POOLED_THREAD, this);

    private final Object lock = new Object();
    private @NotNull PendingFindings pending = new PendingFindings();

    public PendingScannerFindingsChangesService(@NotNull Project project) {
        this.project = project;
    }

    /**
     * Queues changes to findings files to be processed.
     *
     * @param directories Directories containing AppMap findings which changed. A directory causes a complete reload.
     * @param toAdd       Added findings files
     * @param toRefresh   Refreshed findings files
     * @param toRemove    Removed findings files
     */
    public void queueChanges(@NotNull Set<@SystemIndependent String> directories,
                             @NotNull Set<@SystemIndependent String> toAdd,
                             @NotNull Set<@SystemIndependent String> toRefresh,
                             @NotNull Set<@SystemIndependent String> toRemove) {
        synchronized (lock) {
            pending.directories(directories);
            pending.add(toAdd);
            pending.refresh(toRefresh);
            pending.remove(toRemove);

            alarm.cancelAndRequest();
        }
    }

    private void processChanges() {
        // Only process when indexes are available because FindingsManager uses them.
        DumbService.getInstance(project).runReadActionInSmartMode(() -> {
            // Bet pending changes and replace with new changeset, anything added to the new changeset while this method
            // is being executed will be processed wih the next alarm.
            PendingFindings toProcess;
            synchronized (lock) {
                toProcess = this.pending;
                this.pending = new PendingFindings();
            }

            if (!toProcess.isEmpty()) {
                processChanges(toProcess);
            }
        });
    }

    private void processChanges(@NotNull PendingFindings pending) {
        var fs = LocalFileSystem.getInstance();
        var manager = FindingsManager.getInstance(project);

        // If there's at least one modified directory with finding files,
        // then we do a complete reload instead of attempting to find the best diff.
        for (var path : pending.directories) {
            var directory = fs.findFileByPath(path);
            // either a deleted directory, which may have contained AppMaps
            // or a new/modified directory, which contains AppMaps
            if (directory == null || isDirectoryWithFindingFiles(directory)) {
                manager.reloadAsync();
                return;
            }
        }

        for (var filePath : pending.toRemove) {
            if (filePath != null) {
                manager.removeFindingsFile(filePath);
            }
        }

        for (var filePath : pending.toAdd) {
            var file = fs.findFileByPath(filePath);
            if (file != null) {
                manager.addFindingsFile(file);
            }
        }

        for (var filePath : pending.toRefresh) {
            var file = fs.findFileByPath(filePath);
            if (file != null) {
                manager.reloadFindingsFile(file);
            }
        }
    }

    @Override
    public void dispose() {
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

    /**
     * Class to wrap all pending changes in a single object.
     * This is used to avoid that changes are updated while a set of pending changes is being processed.
     */
    @EqualsAndHashCode
    private static class PendingFindings {
        final Set<@SystemIndependent String> directories = Sets.newConcurrentHashSet();
        final Set<@SystemIndependent String> toAdd = Sets.newConcurrentHashSet();
        final Set<@SystemIndependent String> toRefresh = Sets.newConcurrentHashSet();
        final Set<@SystemIndependent String> toRemove = Sets.newConcurrentHashSet();

        boolean isEmpty() {
            return directories.isEmpty() && toAdd.isEmpty() && toRefresh.isEmpty() && toRemove.isEmpty();
        }

        void directories(Collection<@SystemIndependent String> paths) {
            directories.addAll(paths);
        }

        void add(Collection<@SystemIndependent String> paths) {
            toAdd.addAll(paths);
        }

        void refresh(Collection<@SystemIndependent String> paths) {
            toRefresh.addAll(paths);
        }

        void remove(Collection<@SystemIndependent String> paths) {
            toRemove.addAll(paths);
        }
    }

}
