package appland.files;

import appland.utils.GsonUtils;
import com.google.gson.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.concurrency.annotations.RequiresReadLock;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;

/**
 * Wrapper class for a line offset inside a file.
 * The file is defined by either an absolute or a relative file path.
 */
@Value
public class FileLocation {
    /**
     * Parses a relative path with optional line number into a location.
     *
     * @param path The file to parse with ":" as optional delimiter of path and line, e.g. "dir/file.txt:123" or "dir/file.txt".
     * @return The location if the path could be parsed.
     */
    @Nullable
    public static FileLocation parse(@NotNull String path) {
        var lineIndex = path.lastIndexOf(':');
        if (lineIndex == -1) {
            return new FileLocation(path, null);
        }

        try {
            int line = Integer.parseInt(path.substring(lineIndex + 1));
            return new FileLocation(path.substring(0, lineIndex), line);
        } catch (NumberFormatException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("exception parsing offsets of " + path);
            }
            return null;
        }
    }

    private static final Logger LOG = Logger.getInstance("#appmap.file");

    public @NotNull String filePath;
    public @Nullable Integer line;

    public int getZeroBasedLine(int fallback) {
        return line == null ? fallback : line - 1;
    }

    @RequiresReadLock
    public @Nullable VirtualFile resolveFilePath(@NotNull Project project, @NotNull VirtualFile baseFile) {
        try {
            return FileLookup.findRelativeFile(project, baseFile, FileUtil.toSystemIndependentName(filePath));
        } catch (Exception e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("failed to resolve location: " + this, e);
            }
            return null;
        }
    }

    public @NotNull String getSuffix() {
        return line == null ? "" : ":" + line;
    }

    public static class TypeAdapter implements JsonSerializer<FileLocation>, JsonDeserializer<FileLocation> {
        @Override
        public FileLocation deserialize(JsonElement jsonElement,
                                        Type type,
                                        JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            var json = jsonElement.getAsJsonObject();
            var uri = json.getAsJsonObject("uri").getAsJsonPrimitive("path").getAsString();
            Integer line = null;
            var range = json.get("range");
            if (range instanceof JsonObject) {
                line = ((JsonObject) range).getAsJsonPrimitive("line").getAsInt();
            }
            return new FileLocation(uri, line);
        }

        @Override
        public JsonElement serialize(FileLocation fileLocation,
                                     Type type,
                                     JsonSerializationContext jsonSerializationContext) {
            var json = new JsonObject();
            json.add("uri", GsonUtils.singlePropertyObject("path", fileLocation.filePath));
            var line = fileLocation.line;
            if (line != null && line >= 0) {
                json.add("range", GsonUtils.singlePropertyObject("line", line));
            }
            return json;
        }
    }
}
