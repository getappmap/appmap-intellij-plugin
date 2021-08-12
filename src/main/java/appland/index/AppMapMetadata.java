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
    private final int requestCount;
    private final int queryCount;
    private final int functionsCount;

    public AppMapMetadata(@NotNull String name, @NotNull String filepath) {
        this(name, filepath, 0, 0, 0);
    }

    public AppMapMetadata(@NotNull String name, @NotNull String filepath, int requestCount, int queryCount, int functionsCount) {
        this.name = name;
        this.filepath = filepath;
        this.requestCount = requestCount;
        this.queryCount = queryCount;
        this.functionsCount = functionsCount;
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

    public int getRequestCount() {
        return requestCount;
    }

    public int getQueryCount() {
        return queryCount;
    }

    public int getFunctionsCount() {
        return functionsCount;
    }

    @Override
    public String toString() {
        return "AppMapMetadata{" +
                "name='" + name + '\'' +
                ", filepath='" + filepath + '\'' +
                ", requestCount=" + requestCount +
                ", queryCount=" + queryCount +
                ", functionsCount=" + functionsCount +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AppMapMetadata that = (AppMapMetadata) o;
        return requestCount == that.requestCount && queryCount == that.queryCount && functionsCount == that.functionsCount && name.equals(that.name) && filepath.equals(that.filepath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, filepath, requestCount, queryCount, functionsCount);
    }
}
