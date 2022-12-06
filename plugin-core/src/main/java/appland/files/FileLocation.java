package appland.files;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Wrapper class for a line offset inside a file.
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

    public @NotNull String filePath;
    public @Nullable Integer line;

    public int getZeroBasedLine(int fallback) {
        return line == null ? fallback : line - 1;
    }
}
