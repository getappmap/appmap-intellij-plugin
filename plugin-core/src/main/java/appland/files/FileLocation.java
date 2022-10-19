package appland.files;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * Wrapper class for a line offset inside a file.
 */
public final class FileLocation {
    /**
     * Parses a relative path with optional line number into a location.
     *
     * @param path The file to parse with ":" as optional delimiter of path and line, e.g. "dir/file.txt:123" or "dir/file.txt".
     * @return The location if the path could be parsed.
     */
    @Nullable
    public static FileLocation parse(@NotNull String path) {
        if (!path.contains(":")) {
            return new FileLocation(path, null);
        }

        List<String> parts = StringUtil.split(path, ":", true, true);
        if (parts.size() != 2) {
            return null;
        }

        try {
            int line = Integer.parseInt(parts.get(1));
            return new FileLocation(parts.get(0), line);
        } catch (NumberFormatException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("exception parsing offsets of " + path);
            }
            return null;
        }
    }

    private static final Logger LOG = Logger.getInstance("#appmap.file");

    @NotNull
    public final String filePath;
    @Nullable
    public final Integer line;

    FileLocation(@NotNull String filePath, @Nullable Integer line) {
        this.filePath = filePath;
        this.line = line;
    }

    @Override
    public String toString() {
        return "FileLocation{" +
                "filePath='" + filePath + '\'' +
                ", line=" + line +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileLocation location = (FileLocation) o;
        return filePath.equals(location.filePath) && Objects.equals(line, location.line);
    }

    @Override
    public int hashCode() {
        return Objects.hash(filePath, line);
    }
}
