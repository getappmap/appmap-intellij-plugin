package appland.files;

import appland.cli.AppLandCommandLineService;
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
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
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
import java.util.Collection;

public final class AppMapFiles {
    private static final Logger LOG = Logger.getInstance("#appmap.files");

    public static final String APPMAP_YML = "appmap.yml";

    // prune size parameter passed to the appmap CLI tool
    public static final String APPMAP_CLI_MAX_PRUNE_SIZE = "10mb";
    // Files larger than 200mb are considered giant and can't be pruned or opened
    public static int SIZE_THRESHOLD_GIANT_BYTES = 200 * 1024 * 1024;
    // Files larger than 10mb and smaller than 200mb are considered large and have to be pruned
    public static int SIZE_THRESHOLD_LARGE_BYTES = 10 * 1024 * 1024;

    private AppMapFiles() {
    }

    public static boolean isAppMapConfigFileName(@NotNull String fileName) {
        return FileUtil.fileNameEquals(fileName, APPMAP_YML);
    }

    /**
     * @return The appmap.yaml files available in the current project and search scope.
     */
    @RequiresReadLock
    public static @NotNull Collection<VirtualFile> findAppMapConfigFiles(@NotNull Project project, @NotNull GlobalSearchScope scope) {
        return FilenameIndex.getVirtualFilesByName(project, APPMAP_YML, false, scope);
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
     * @param appMapDataFile Source file located in the appmap directory created by indexer or scanner.
     * @return The appmap.json file, which is the source of the data in the appmap directory
     */
    public static @Nullable VirtualFile findAppMapSourceFile(@NotNull VirtualFile appMapDataFile) {
        var parent = appMapDataFile.getParent();
        return parent == null
                ? null
                : parent.findFileByRelativePath("../" + FileUtil.getNameWithoutExtension(parent.getName()) + ".appmap.json");
    }

    /**
     * @param appMapFile An .appmap.json file
     * @return The matching appmap-findings.json file for the given AppMap or {@code null} if unavailable.
     */
    @RequiresReadLock
    public static @Nullable VirtualFile findRelatedFindingsFile(@NotNull VirtualFile appMapFile) {
        var parent = appMapFile.getParent();
        // name.appmap.json is mapped to name/appmap-findings.json
        var findingsPath = String.format("%s/appmap-findings.json", StringUtil.trimEnd(appMapFile.getName(), ".appmap.json"));
        return parent.findFileByRelativePath(findingsPath);
    }

    /**
     * Load the content of the file.
     *
     * @param file File to load
     * @return File content or {@code null} if an error ocurred
     */
    @RequiresReadLock
    public static @Nullable String loadFileContent(@NotNull VirtualFile file) {
        return ReadAction.compute(() -> {
            var document = FileDocumentManager.getInstance().getDocument(file);
            if (document == null) {
                LOG.error("unable to retrieve document for file: " + file.getPath());
                return null;
            }

            return document.getText();
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

    @RequiresBackgroundThread
    public static @Nullable String loadAppMapStats(@NotNull VirtualFile file) {
        var command = AppLandCommandLineService.getInstance().createAppMapStatsCommand(file);
        if (command == null) {
            return null;
        }

        try {
            var processOutput = ExecUtil.execAndGetOutput(command);
            return getOutputOrLogStatus(command, processOutput);
        } catch (ExecutionException e) {
            LOG.debug("error retrieving AppMap file stats: " + file.getPath(), e);
        }
        return null;
    }

    private static @Nullable String getOutputOrLogStatus(@NotNull GeneralCommandLine command, @NotNull ProcessOutput processOutput) {
        if (processOutput.getExitCode() == 0) {
            return processOutput.getStdout();
        }

        LOG.debug("failed to execute command: " + command.getCommandLineString() + ", status: " + processOutput);
        return null;
    }
}
