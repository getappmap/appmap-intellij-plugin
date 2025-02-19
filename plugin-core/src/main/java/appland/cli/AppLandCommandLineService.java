package appland.cli;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
     *
     * @param directory                 Directory, where the service should be launched.
     * @param waitForProcessTermination Wait for termination of processes, which are no longer needed. This is mostly useful for test cases.
     */
    void start(@NotNull VirtualFile directory, boolean waitForProcessTermination) throws ExecutionException;

    /**
     * Stops any running service, which is being executed for the given directory.
     *
     * @param directory          Directory path
     * @param waitForTermination Waits until the process has been terminated. This is mostly useful for test cases.
     */
    void stop(@NotNull VirtualFile directory, boolean waitForTermination);

    /**
     * Refreshes the processes for the currently open projects.
     * It stops processes, which are not required anymore and launches services for yet unhandled content roots.
     */
    @RequiresBackgroundThread
    void refreshForOpenProjects();

    /**
     * Helper to launch {@link #refreshForOpenProjects} in a background thread.
     */
    void refreshForOpenProjectsInBackground();

    /**
     * Stops running processes and then starts processes for the enabled roots.
     */
    void restartProcessesInBackground();

    /**
     * @return List of directories, which are handled by active CLI processes.
     */
    @NotNull List<VirtualFile> getActiveRoots();

    /**
     * @return The closest parent directory, which is configured with an appmap.yml file for the given AppMap file.
     */
    @Nullable VirtualFile getActiveRoot(@NotNull VirtualFile appMapFile);

    /**
     * Stop all processes.
     *
     * @param waitForTermination Wait for process termination. This is mostly useful for test cases.
     */
    void stopAll(boolean waitForTermination);

    /**
     * Stop all processes.
     *
     * @param timeout  Length of timeout to wait for process termination. 0 disables the waiting for termination.
     * @param timeUnit Unit of timeout.
     */
    void stopAll(int timeout, @NotNull TimeUnit timeUnit);

    /**
     * Creates the command line to install AppMap in the given directory.
     *
     * @param path     Project path
     * @param language Detected Language
     * @return The command line or {@code null} if the CLI is unavailable
     */
    @Nullable GeneralCommandLine createInstallCommand(@NotNull Path path, @NotNull String language);

    /**
     * Creates the command line to generate an OpenAPI file for a given workspace directory.
     *
     * @param projectRoot Project path
     * @return The command line or {@code null} if the CLI is unavailable
     */
    @Nullable GeneralCommandLine createGenerateOpenApiCommand(@NotNull VirtualFile projectRoot);

    /**
     * Creates the command line to prune an AppMap file.
     *
     * @param appMapFile The file to prune. It won't be modified by the command, but the pruned content will be sent to STDOUT. Only files on the local file system are supported.
     * @param maxSize    The maximum size of the pruned file, must be a value understood by the CLI, e.g. "10mb"
     * @return The command line or {@code null} if the CLI is unavailable
     */
    @Nullable GeneralCommandLine createPruneAppMapCommand(@NotNull VirtualFile appMapFile, @NotNull String maxSize);

    /**
     * Creates the command line to provide stats about an AppMap file.
     *
     * @param appMapFile The AppMap file to load
     * @return The command line or {@code null} if the CLI is unavailable
     */
    @Nullable GeneralCommandLine createAppMapStatsCommand(@NotNull VirtualFile appMapFile);

    /**
     * Creates the command line to launch the AppMap JSON-RPC server.
     *
     * @param port If non-null, then the JSON-RPC server will be launched with this port.
     *             This is useful to restart a server on the same port.
     * @return The command line or {@code null} if the CLI is unavailable
     */
    @Nullable GeneralCommandLine createAppMapJsonRpcCommand(@Nullable Integer port);
}
