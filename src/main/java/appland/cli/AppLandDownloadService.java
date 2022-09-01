package appland.cli;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Application service managing the downloads of CLI binaries.
 */
public interface AppLandDownloadService {
    static @NotNull AppLandDownloadService getInstance() {
        return ApplicationManager.getApplication().getService(AppLandDownloadService.class);
    }

    /**
     * @param type The type of CLI tool
     * @return The location on disk, where the binary for the given tool type is stored. Only existing file paths are returned.
     */
    @Nullable Path getDownloadFilePath(@NotNull CliTool type);

    /**
     * @param type Type of commandline tool
     * @return {@code true} if there's a downloaded binary of the given tool with any version
     */
    boolean isDownloaded(@NotNull CliTool type);

    /**
     * @param type         Type of commandline tool
     * @param version      The required version, e.g. 1.2.3
     * @param unitTestMode To determine the target download location
     * @return {@code true} if there's a downloaded binary matching type and version
     */
    boolean isDownloaded(@NotNull CliTool type, @NotNull String version, boolean unitTestMode);

    /**
     * Download a new copy of the tool binary and store it at the expected location on disk.
     * It notifies via {@link AppLandDownloadListener} when the download completed.
     *
     * @param type              Type of commandline tool
     * @param version           Version to download
     * @param progressIndicator Indicator to show the download progress
     * @return {@code true} if the download succeeded
     */
    @RequiresBackgroundThread
    boolean download(@NotNull CliTool type, @NotNull String version, @NotNull ProgressIndicator progressIndicator);

    /**
     * Fetch metadata about the latest available version
     *
     * @param type Type of command line tool
     * @return Available version of {@code null} if the retrieval was unsuccessful
     */
    @RequiresBackgroundThread
    @Nullable String fetchLatestRemoteVersion(@NotNull CliTool type) throws IOException;

    /**
     * Starts tasks to download the latest available versions of the needed CLI binaries.
     */
    void queueDownloadTasks(@NotNull Project project) throws IOException;
}
