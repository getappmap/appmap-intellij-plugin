package appland.files;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class AppMapFiles {
    private static final Logger LOG = Logger.getInstance("#appmap.files");

    private AppMapFiles() {
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
     * @return {@code true} if the operation was successful
     */
    public static boolean updateMetadata(@NotNull Path appMapFile, @NotNull String name) {
        try {
            var gson = new GsonBuilder().create();
            var json = gson.fromJson(Files.newBufferedReader(appMapFile, StandardCharsets.UTF_8), JsonObject.class);
            if (!json.has("metadata")) {
                json.add("metadata", new JsonObject());
            }
            var metadata = json.getAsJsonObject("metadata");
            metadata.addProperty("name", name);

            Files.write(appMapFile, gson.toJson(json).getBytes(StandardCharsets.UTF_8));
            return true;
        } catch (IOException e) {
            LOG.debug("error while updating AppMap metadata", e);
            return false;
        }
    }

    /**
     * @param dir          Parent dir
     * @param metadataName Descriptive name, used to calculate the filename
     * @return A filename for a new appmap stored inside of dir.
     */
    public static Path appMapFilename(@NotNull Path dir, @NotNull String metadataName) {
        var filename = metadataName.replaceAll("[^a-zA-Z0-9]", "_");
        if (metadataName.isEmpty()) {
            throw new IllegalArgumentException("AppMap filename must not be empty");
        }

        var candidate = String.format("%s.appmap.json", filename);
        var i = 1;
        while (Files.exists(dir.resolve(candidate))) {
            candidate = String.format("%s(%d).appmap.json", filename, i);
            i++;
        }
        return dir.resolve(candidate);
    }
}
