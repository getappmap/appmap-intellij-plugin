package appland.files;

import appland.cli.AppLandCommandLineService;
import appland.index.AppMapSearchScopes;
import appland.utils.GsonUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.gson.JsonObject;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.execution.util.ExecUtil;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread;
import com.intellij.util.concurrency.annotations.RequiresReadLock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class AppMapFiles {
    private static final Logger LOG = Logger.getInstance("#appmap.files");

    public static final String APPMAP_YML = "appmap.yml";

    // prune size parameter passed to the appmap CLI tool
    public static final String APPMAP_CLI_MAX_PRUNE_SIZE = "10mb";
    // Files larger than 200mb are considered giant and can't be pruned or opened
    public static final int SIZE_THRESHOLD_GIANT_BYTES = 200 * 1024 * 1024;
    // Files larger than 10mb and smaller than 200mb are considered large and have to be pruned
    public static final int SIZE_THRESHOLD_LARGE_BYTES = 10 * 1024 * 1024;
    // Maximum time to wait for the CLI stats command to finish
    private static final int STATS_COMMAND_TIMEOUT_MILLIS = 20_000;

    private AppMapFiles() {
    }

    public static boolean isAppMapConfigFileName(@NotNull String fileName) {
        return FileUtilRt.fileNameEquals(fileName, APPMAP_YML);
    }

    /**
     * Locate the appmap.yml files located in the project.
     * This method must be invoked in a {@link ReadAction}.
     * The appmap.yml files are not searched inside excluded folders, libraries or dependencies of the project.
     *
     * @return The appmap.yaml files available in the current project.
     * @throws com.intellij.openapi.project.IndexNotReadyException If the filename index it unavailable
     */
    @RequiresReadLock
    public static @NotNull Collection<VirtualFile> findAppMapConfigFiles(@NotNull Project project) {
        return findAppMapConfigFiles(AppMapSearchScopes.appMapConfigSearchScope(project));
    }

    /**
     * Locate the appmap.yml files located in the project and scope.
     * This method must be invoked in a {@link ReadAction}.
     *
     * @return The appmap.yaml files available in the current project and search scope.
     * @throws com.intellij.openapi.project.IndexNotReadyException If the filename index it unavailable
     */
    @RequiresReadLock
    public static @NotNull Collection<VirtualFile> findAppMapConfigFiles(@NotNull GlobalSearchScope scope) {
        return FilenameIndex.getVirtualFilesByName(APPMAP_YML, false, scope);
    }

    /**
     * @param appMapConfigFile appmap.yml file
     * @return The "appmap_dir" property value, if it's configured in the file
     */
    public static @Nullable String readAppMapDirConfigValue(@NotNull VirtualFile appMapConfigFile) {
        assert appMapConfigFile.isInLocalFileSystem();
        assert !appMapConfigFile.isDirectory();

        try {
            var content = VfsUtil.loadText(appMapConfigFile);
            var tree = new ObjectMapper(new YAMLFactory()).readTree(content);
            var appMapDir = tree.get("appmap_dir");
            if (appMapDir != null && appMapDir.isTextual()) {
                return StringUtil.nullize(appMapDir.textValue());
            }
        } catch (Exception e) {
            LOG.debug("error parsing appmap.yml configuration file", e);
        }
        return null;
    }

    /**
     * @return {@code true} if the given file is an AppMap.
     */
    public static boolean isAppMap(@NotNull VirtualFile file) {
        return file.isValid() && !file.isDirectory() && isAppMapFileName(file.getName());
    }

    public static boolean isAppMapFileName(@NotNull String fileName) {
        return fileName.endsWith(".appmap.json");
    }

    /**
     * Updates  JSON property "metadata.name" with the given name. The updated data is replacing the data of appMapFile.
     *
     * @param appMapFile The path to the file
     * @param name       The new name
     * @param charset    Charset of the AppMap file
     * @return {@code true} if the operation was successful
     */
    public static boolean updateMetadata(@NotNull Path appMapFile, @NotNull String name, @NotNull Charset charset) {
        try {
            var json = GsonUtils.GSON.fromJson(Files.newBufferedReader(appMapFile, charset), JsonObject.class);
            if (!json.has("metadata")) {
                json.add("metadata", new JsonObject());
            }
            var metadata = json.getAsJsonObject("metadata");
            metadata.addProperty("name", name);

            Files.write(appMapFile, GsonUtils.GSON.toJson(json).getBytes(charset));
            return true;
        } catch (IOException e) {
            LOG.debug("error while updating AppMap metadata of " + appMapFile, e);
            return false;
        }
    }

    /**
     * @param parentDirectoryPath Parent dir
     * @param metadataName        Descriptive name, used to calculate the filename
     * @return A filename for a new AppMap stored inside of dir.
     */
    public static @NotNull Path appMapFilename(@NotNull Path parentDirectoryPath, @NotNull String metadataName) {
        if (metadataName.isEmpty()) {
            throw new IllegalArgumentException("AppMap filename must not be empty");
        }

        var filename = metadataName.replaceAll("[^a-zA-Z0-9]", "_");
        var candidate = String.format("%s.appmap.json", filename);
        var i = 1;
        while (Files.exists(parentDirectoryPath.resolve(candidate))) {
            candidate = String.format("%s(%d).appmap.json", filename, i);
            i++;
        }
        return parentDirectoryPath.resolve(candidate);
    }

    /**
     * @param appMapMetadataFile Source file located in the appmap directory created by indexer or scanner.
     * @return The .appmap.json file, which is the source of the given metadata file
     */
    @RequiresReadLock
    public static @Nullable VirtualFile findAppMapFileByMetadataFile(@Nullable VirtualFile appMapMetadataFile) {
        if (appMapMetadataFile == null) {
            return null;
        }
        return findAppMapByMetadataDirectory(appMapMetadataFile.getParent());
    }

    /**
     * @param appMapMetadataDirectory Directory containing AppMap metadata files created by the AppMap CLI tools
     * @return The .appmap.json file, which is the source of the metadata directory.
     */
    @RequiresReadLock
    public static @Nullable VirtualFile findAppMapByMetadataDirectory(@Nullable VirtualFile appMapMetadataDirectory) {
        if (appMapMetadataDirectory == null) {
            return null;
        }

        var appMapFilePath = "../" + FileUtil.getNameWithoutExtension(appMapMetadataDirectory.getName()) + ".appmap.json";
        return appMapMetadataDirectory.findFileByRelativePath(appMapFilePath);
    }

    /**
     * @param appMapFile An .appmap.json file
     * @return The matching appmap-findings.json file for the given AppMap or {@code null} if unavailable.
     */
    @RequiresReadLock
    public static @Nullable VirtualFile findRelatedFindingsFile(@NotNull VirtualFile appMapFile) {
        var directory = findAppMapMetadataDirectory(appMapFile);
        return directory != null ? directory.findChild("appmap-findings.json") : null;
    }

    /**
     * @param appMapFile An .appmap.json file
     * @return Directory containing the metadata JSON files created by the AppMap CLI tools.
     */
    @RequiresReadLock
    public static @Nullable VirtualFile findAppMapMetadataDirectory(@NotNull VirtualFile appMapFile) {
        var directoryName = StringUtil.trimEnd(appMapFile.getName(), ".appmap.json");
        return appMapFile.getParent().findFileByRelativePath(directoryName);
    }

    /**
     * Load the content of the file.
     *
     * @param file File to load
     * @return File content or {@code null} if an error occurred
     */
    @RequiresReadLock
    public static @Nullable String loadFileContent(@NotNull VirtualFile file) {
        return ReadAction.compute(() -> {
            if (!file.isValid()) {
                return null;
            }

            try {
                return VfsUtilCore.loadText(file);
            } catch (IOException e) {
                LOG.error("unable to load AppMap file content: " + file.getPath());
                return null;
            }
        });
    }

    /**
     * Loads the AppMap file content and follows the same rules as VSCode:
     * <ol>
     * <li>size <= 10mb: load as-is</li>
     * <li>size > 200mb: return empty content</li>
     * <li>10mb < size <= 200mb: prune JSON using the AppMap CLI tool</li>
     * </ol>
     *
     * @param file AppMap file to load
     * @return The content of {@code null} if an error occurred loading the content
     */
    @RequiresBackgroundThread
    public static @Nullable String loadAppMapFile(@NotNull VirtualFile file) {
        return loadAppMapFile(file, SIZE_THRESHOLD_GIANT_BYTES, SIZE_THRESHOLD_LARGE_BYTES, APPMAP_CLI_MAX_PRUNE_SIZE);
    }

    @RequiresReadLock
    public static @Nullable VirtualFile findTopLevelContentRoot(@NotNull Project project,
                                                                @NotNull VirtualFile insideContentRoot) {
        for (VirtualFile root : findTopLevelContentRoots(project)) {
            if (VfsUtilCore.isAncestor(root, insideContentRoot, false)) {
                return root;
            }
        }
        return null;
    }

    @RequiresReadLock
    public static @NotNull VirtualFile[] findTopLevelContentRoots(@NotNull Project project) {
        var roots = new ArrayList<>(List.of(ProjectRootManager.getInstance(project).getContentRoots()));
        roots.sort(Comparator.comparingInt(o -> o.getPath().length()));

        var visited = new HashSet<VirtualFile>();
        for (var iterator = roots.iterator(); iterator.hasNext(); ) {
            var root = iterator.next();
            if (VfsUtil.isUnder(root, visited)) {
                iterator.remove();
            } else {
                visited.add(root);
            }
        }

        return roots.toArray(VirtualFile.EMPTY_ARRAY);
    }

    /**
     * File appmap.yml is a marker file to tell that CLI processes may be launched for a directory.
     *
     * @return {@code true} if the directory contains a appmap.yml file and may have CLI processes watching it.
     */
    public static boolean isDirectoryEnabled(@NotNull VirtualFile directory) {
        return directory.isValid() && directory.findChild(APPMAP_YML) != null;
    }

    /**
     * Load AppMap with specific size threshold values.
     *
     * @param file                    File to load
     * @param sizeThresholdGiantBytes Files above this size are considered "giant"
     * @param sizeThresholdLargeBytes Files above this size are considered "large", unless they're "giant"
     * @param appMapTargetSize        Target size specifier for the pruned AppMap file
     * @return Content of the AppMap file, pruned when necessary. Either {@code null} in case of errors or valid JSON.
     */
    @RequiresBackgroundThread
    static @Nullable String loadAppMapFile(@NotNull VirtualFile file,
                                           int sizeThresholdGiantBytes,
                                           int sizeThresholdLargeBytes,
                                           @NotNull String appMapTargetSize) {
        var fileLength = file.getLength();

        if (fileLength > sizeThresholdGiantBytes) {
            return "{}";
        }

        if (fileLength > sizeThresholdLargeBytes) {
            var command = AppLandCommandLineService.getInstance().createPruneAppMapCommand(file, appMapTargetSize);
            if (command != null) {
                try {
                    var processOutput = ExecUtil.execAndGetOutput(command);
                    return getOutputOrLogStatus(command, processOutput);
                } catch (ExecutionException e) {
                    LOG.debug("error pruning AppMap: " + file.getPath(), e);
                }
            }
            return null;
        }

        return loadFileContent(file);
    }

    /**
     * Executes the stats command with the AppMap CLI and returns STDOUT if the command executed successfully.
     * The maximum runtime is limited to {@link #STATS_COMMAND_TIMEOUT_MILLIS} milliseconds.
     *
     * @param file The file to pass to the stats command
     * @return STDOUT of the process if it executed successfully
     */
    @RequiresBackgroundThread
    public static @Nullable String loadAppMapStats(@NotNull VirtualFile file) {
        var command = AppLandCommandLineService.getInstance().createAppMapStatsCommand(file);
        if (command == null) {
            return null;
        }

        try {
            var processOutput = ExecUtil.execAndGetOutput(command, STATS_COMMAND_TIMEOUT_MILLIS);
            return getOutputOrLogStatus(command, processOutput);
        } catch (ExecutionException e) {
            LOG.debug("error retrieving AppMap file stats: " + file.getPath(), e);
        }
        return null;
    }

    private static @Nullable String getOutputOrLogStatus(@NotNull GeneralCommandLine command, @NotNull ProcessOutput processOutput) {
        if (processOutput.getExitCode() == 0 && !processOutput.isTimeout()) {
            return processOutput.getStdout();
        }

        LOG.debug("failed to execute command: " + command.getCommandLineString() + ", status: " + processOutput);
        return null;
    }
}
