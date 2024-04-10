package appland.javaAgent;

import appland.AppMapBundle;
import appland.utils.SystemProperties;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.PerformInBackgroundOption;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.util.ThrowableRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Application service to manage the download of the AppMap Java agent.
 */
@Service(Service.Level.APP)
public final class AppMapJavaAgentDownloadService {
    private static final Logger LOG = Logger.getInstance(AppMapJavaAgentDownloadService.class);
    private static final String AGENT_LINK_FILENAME = "appmap.jar";

    public static AppMapJavaAgentDownloadService getInstance() {
        return ApplicationManager.getApplication().getService(AppMapJavaAgentDownloadService.class);
    }

    /**
     * @return Local path to the downloaded Java agent file.
     * {@code null} is returned if the agent has not been downloaded yet.
     */
    public @Nullable Path getJavaAgentPathIfExists() {
        var jarFilePath = getJavaAgentPath();
        return jarFilePath == null || Files.notExists(jarFilePath) ? null : jarFilePath;
    }

    @SuppressWarnings("DialogTitleCapitalization")
    public void downloadJavaAgent(@NotNull Project project) {
        var title = AppMapBundle.get("javaAgent.download.title");
        new Task.Backgroundable(project, title, false, PerformInBackgroundOption.ALWAYS_BACKGROUND) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    downloadJavaAgentSync(indicator);
                } catch (IOException e) {
                    // User should have been warned already, so log at DEBUG
                    LOG.debug(String.format("failed to download AppMap Java agent: %s (%s)", e, e.getCause()));
                }
            }
        }.queue();
    }

    public boolean downloadJavaAgentSync(@NotNull ProgressIndicator indicator) throws IOException {
        var agentFilePath = getJavaAgentPath();
        if (agentFilePath == null) {
            LOG.warn("unable to locate target file path for AppMap Java agent");
            return false;
        }

        var latestAsset = getLatestAsset(indicator);
        if (latestAsset == null) {
            return false;
        }

        // e.g. ~/.appmap/lib/java/appmap-1.7.2.jar
        var assetDownloadPath = agentFilePath.resolveSibling(latestAsset.getFileName());

        // don't download if the asset file does already exist
        if (Files.exists(assetDownloadPath)) {
            LOG.info(String.format("%s already exists, not downloading", assetDownloadPath));
            return false;
        }

        // e.g. appmap-1.7.2.jar.downloading
        var lockFilePath = assetDownloadPath.resolveSibling(latestAsset.getFileName() + ".downloading");
        assert !assetDownloadPath.equals(lockFilePath);

        if (!guardWithLockFile(lockFilePath, () -> downloadAgentJarFile(indicator, latestAsset, assetDownloadPath))) {
            return false;
        }

        // create a relative symbolic appmap.jar pointing to the downloaded JAR file
        try {
            Files.deleteIfExists(agentFilePath);
            Files.createSymbolicLink(agentFilePath, agentFilePath.getParent().relativize(assetDownloadPath));
        } catch (IOException e) {
            // copy the downloaded file to "agent.jar" as a fallback,
            // e.g. on system where symbolic links are unsupported
            Files.copy(assetDownloadPath, agentFilePath);
        }

        return true;
    }

    @Nullable
    private static Release.Asset getLatestAsset(@NotNull ProgressIndicator indicator) {
        final Release[] releases = {MavenRelease.INSTANCE, GitHubRelease.INSTANCE};
        for (Release release : releases) {
            try {
                var latestAsset = release.getLatest(indicator)
                        .stream()
                        .filter(asset -> "application/java-archive".equals(asset.getContentType()))
                        .findFirst()
                        .orElse(null);

                if (latestAsset != null) {
                    LOG.info("Got latest asset URL: " + latestAsset.getDownloadUrl());
                    return latestAsset;
                }
            } catch (IOException e) {
                LOG.info(String.format("Failed to query release, %s: %s", release, e));
            }
        }

        LOG.warn("Couldn't query any source for latest release.");
        return null;
    }

    /**
     * Guards the operation of the runnable with a lock file.
     * If the lock file does already exist and is less than 5 minutes old, then the runnable is NOT executed.
     * Otherwise, the lock file is either created or updated before the runnable is executed.
     *
     * @param lockFilePath Target path of the lock file
     * @param runnable     The operation to guard with the lock file
     * @return {@code true} if the runnable was executed
     * @throws IOException Thrown if the runnable failed to execute
     */
    private boolean guardWithLockFile(@NotNull Path lockFilePath,
                                      @NotNull ThrowableRunnable<IOException> runnable) throws IOException {
        if (Files.exists(lockFilePath)) {
            var fiveMinutesAgo = Instant.now().minus(5, ChronoUnit.MINUTES);
            if (!Files.getLastModifiedTime(lockFilePath).toInstant().isBefore(fiveMinutesAgo)) {
                return false;
            }

            Files.setLastModifiedTime(lockFilePath, FileTime.from(Instant.now()));
        } else {
            Files.createFile(lockFilePath);
        }

        try {
            runnable.run();
        } finally {
            Files.deleteIfExists(lockFilePath);
        }
        return true;
    }

    /**
     * Downloads the GitHub asset and stores it at the given path.
     *
     * @param indicator         Progress indicator
     * @param agentReleaseAsset Asset to download
     * @param assetDownloadPath Target path, where the download is stored on disk
     * @throws IOException Thrown if the download failed to complete
     */
    private static void downloadAgentJarFile(@NotNull ProgressIndicator indicator,
                                             @NotNull Release.Asset agentReleaseAsset,
                                             @NotNull Path assetDownloadPath) throws IOException {
        try {
            agentReleaseAsset.download(indicator, assetDownloadPath);
            LOG.info(String.format("Downloaded %s", agentReleaseAsset.getDownloadUrl()));

        } catch (IOException e) {
            // remove partial download and throw IOException
            Files.deleteIfExists(assetDownloadPath);

            // Getting here means that we were able to determine that there's a
            // new version of the release available, but we couldn't download
            // it. Warn the user that this happened.
            LOG.warn("Failed to download agent JAR " + e);
            LOG.debug(e);
            throw new IOException("Failed to download agent JAR", e);
        }
    }

    private @Nullable Path getJavaAgentPath() {
        var agentDir = getOrCreateAgentDir();
        return agentDir == null ? null : agentDir.resolve(AGENT_LINK_FILENAME);
    }

    /**
     * @return The path of the directory, where the AppMap Java agent should be stored (~/.appmap/lib/java).
     */
    @Nullable Path getOrCreateAgentDir() {
        var agentDirPath = Paths.get(SystemProperties.getUserHome()).resolve(Paths.get(".appmap", "lib", "java"));
        try {
            Files.createDirectories(agentDirPath);
            return agentDirPath;
        } catch (IOException e) {
            LOG.warn("error creating AppMap agent directory: " + agentDirPath, e);
            return null;
        }
    }
}
