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
     * @param type     The type of CLI tool
     * @param platform The platform to search for
     * @param arch     The architecture to search for
     * @return The location on disk, where the binary for the given tool type is stored.
     * It's possible that the file does not exist yet.
     */
    @Nullable Path getDownloadFilePath(@NotNull CliTool type, @NotNull String platform, @NotNull String arch);

    /**
     * Download a new copy of the tool binary and store it at the expected location on disk.
     * It notifies via {@link AppLandDownloadListener} when the download completed.
     *
     * @param type              Type of commandline tool
     * @param version           Version to download
     * @param progressIndicator Indicator to show the download progress
     * @return A {@link AppMapDownloadStatus} value indicating the result of the download.
     */
    @RequiresBackgroundThread
    @NotNull AppMapDownloadStatus download(@NotNull CliTool type,
                                           @NotNull String version,
                                           @NotNull ProgressIndicator progressIndicator);

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

    /**
     * Fetch the latest version that has been downloaded.
     *
     * @param type Type of command line tool
     * @return Latest available version or {@code null} if the retrieval was unsuccessful
     */
    @Nullable String findLatestDownloadedVersion(@NotNull CliTool type);
}
