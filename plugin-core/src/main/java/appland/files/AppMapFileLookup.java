package appland.files;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Extension point to locate files referenced in AppMaps, e.g. from external libraries.
 */
public interface AppMapFileLookup {
    ExtensionPointName<AppMapFileLookup> EP_NAME = ExtensionPointName.create("appland.files.fileLookup");

    /**
     * Context data for file lookup.
     *
     * @param relativePath Path from AppMap, typically relative to appmap.yml
     * @param line         1-based line number from AppMap, or null if not available
     */
    record Data(@NotNull String relativePath, @Nullable Integer line) {
    }

    /**
     * @param project Current project
     * @param data    Context data for the lookup
     * @return The target file, if found by this contributor.
     */
    @Nullable
    VirtualFile findFile(@NotNull Project project, @NotNull Data data);
}
