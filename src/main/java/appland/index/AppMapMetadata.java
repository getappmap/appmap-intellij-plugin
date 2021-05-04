package appland.index;

import com.intellij.util.PathUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Contains metadata about a single AppMap.
 */
public final class AppMapMetadata {
    @NotNull
    private final String name;
    @NotNull
    private final String filepath;

    public AppMapMetadata(@NotNull String name, @NotNull String filepath) {
        this.name = name;
        this.filepath = filepath;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    public String getSystemIndependentFilepath() {
        return filepath;
    }

    @NotNull
    public String getFilename() {
        return PathUtil.getFileName(filepath);
    }

    @Override
    public String toString() {
        return "AppMapMetadata{" +
                "name='" + name + '\'' +
                ", filepath='" + filepath + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AppMapMetadata that = (AppMapMetadata) o;
        return Objects.equals(name, that.name) && Objects.equals(filepath, that.filepath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, filepath);
    }
}
