package appland.files;

import appland.utils.GsonUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.gson.JsonObject;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
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

    private AppMapFiles() {
    }

    public static boolean isAppMapConfigFileName(@NotNull String fileName) {
        return FileUtil.fileNameEquals(fileName, AppMapFiles.APPMAP_YML);
    }

    /**
     * @return The appmap.yaml files available in the current project and search scope.
     */
    @RequiresReadLock
    public static @NotNull Collection<VirtualFile> findAppMapConfigFiles(@NotNull Project project, @NotNull GlobalSearchScope scope) {
        return FilenameIndex.getVirtualFilesByName(project, AppMapFiles.APPMAP_YML, false, scope);
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
}
