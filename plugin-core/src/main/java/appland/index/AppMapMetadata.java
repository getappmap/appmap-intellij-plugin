package appland.index;

import com.intellij.util.PathUtil;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Contains metadata about a single AppMap.
 */
@EqualsAndHashCode
@ToString
public final class AppMapMetadata {
    /**
     * The name defined by the metadata.
     */
    @Getter
    private final @NotNull String name;
    /**
     * System-independent file path.
     */
    private final @NotNull String filepath;
    /**
     * source_location defined by the metadata, relative/path:lineNumber pointing to the entry-point of the AppMap.
     */
    @Getter
    private final @Nullable String sourceLocation;
    @Getter
    private final int requestCount;
    @Getter
    private final int queryCount;
    @Getter
    private final int functionsCount;

    public AppMapMetadata(@NotNull String name, @NotNull String filepath, @Nullable String sourceLocation) {
        this(name, sourceLocation, filepath, 0, 0, 0);
    }

    public AppMapMetadata(@NotNull String name, @Nullable String sourceLocation, @NotNull String filepath, int requestCount, int queryCount, int functionsCount) {
        this.name = name;
        this.sourceLocation = sourceLocation;
        this.filepath = filepath;
        this.requestCount = requestCount;
        this.queryCount = queryCount;
        this.functionsCount = functionsCount;
    }

    @NotNull
    public String getSystemIndependentFilepath() {
        return filepath;
    }

    @NotNull
    public String getFilename() {
        return PathUtil.getFileName(filepath);
    }

    public boolean hasAnyCount() {
        return requestCount > 0 || queryCount > 0 || functionsCount > 0;
    }

    public int getSortCount() {
        return requestCount * 100 + queryCount * 100 + functionsCount * 100;
    }
}
