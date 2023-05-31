package appland.index;

import com.intellij.util.PathUtil;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

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
    private final @NotNull String systemIndependentFilePath;
    @Getter
    private final int requestCount;
    @Getter
    private final int queryCount;
    @Getter
    private final int functionsCount;

    public AppMapMetadata(@NotNull String name, @NotNull String systemIndependentFilePath) {
        this(name, systemIndependentFilePath, 0, 0, 0);
    }

    public AppMapMetadata(@NotNull String name, @NotNull String systemIndependentFilePath, int requestCount, int queryCount, int functionsCount) {
        this.name = name;
        this.systemIndependentFilePath = systemIndependentFilePath;
        this.requestCount = requestCount;
        this.queryCount = queryCount;
        this.functionsCount = functionsCount;
    }

    @NotNull
    public String getSystemIndependentFilepath() {
        return systemIndependentFilePath;
    }

    @NotNull
    public String getFilename() {
        return PathUtil.getFileName(systemIndependentFilePath);
    }

    public boolean hasAnyCount() {
        return requestCount > 0 || queryCount > 0 || functionsCount > 0;
    }

    public int getSortCount() {
        return requestCount * 100 + queryCount * 100 + functionsCount * 100;
    }
}
