package appland.cli;

import com.intellij.execution.ExecutionException;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface AppLandCommandLineService extends Disposable {
    static @NotNull AppLandCommandLineService getInstance() {
        return ApplicationManager.getApplication().getService(AppLandCommandLineService.class);
    }

    /**
     * @param directory Directory
     * @param strict    {@code true} if the directory has to match exactly or if a process serving a parent directory is valid
     * @return {@code true} if there's a process handling directory
     */
    boolean isRunning(@NotNull VirtualFile directory, boolean strict);

    /**
     * Launches the AppLand CLI tools for the given directory.
     * It'll stop any running service, which is serving a subdirectory the given path.
     *
     * @param directory Directory, where the service should be launched.
     */
    void start(@NotNull VirtualFile directory) throws ExecutionException;

    /**
     * Stops any running service, which is being executed for the given directory.
     *
     * @param directory Directory path
     */
    void stop(@NotNull VirtualFile directory);

    /**
     * Refreshes the processes for the currently open projects.
     * It stops processes, which are not required anymore and launches services for yet unhandled content roots.
     */
    void refreshForOpenProjects();

    /**
     * @return List of directories, which are handled by active CLI processes.
     */
    @NotNull List<VirtualFile> getActiveRoots();

    /**
     * Stop all processes.
     */
    void stopAll();
}
