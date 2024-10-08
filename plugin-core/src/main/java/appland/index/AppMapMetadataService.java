package appland.index;

import appland.files.AppMapFiles;
import appland.problemsView.model.TestStatus;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.concurrency.annotations.RequiresReadLock;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Provides {@link AppMapMetadata} by leveraging our  indexes of the AppMap metadata files created by the AppMap CLI tools.
 */
@AllArgsConstructor
@Data
public class AppMapMetadataService {
    public static @NotNull AppMapMetadataService getInstance(@NotNull Project project) {
        return project.getService(AppMapMetadataService.class);
    }

    private final @NotNull Project project;

    /**
     * @return List of all AppMaps of the current project.
     */
    @RequiresReadLock
    public @NotNull List<AppMapMetadata> findAppMaps() {
        return findAppMaps(null);
    }

    /**
     * @param nameFilter String, which must be contained in the name of the returned AppMaps
     * @return List of AppMaps of the current project, which match the given name filter.
     */
    @RequiresReadLock
    public @NotNull List<AppMapMetadata> findAppMaps(@Nullable String nameFilter) {
        return findAppMaps(nameFilter, Integer.MAX_VALUE);
    }

    /**
     * @param nameFilter String, which must be contained in the name of the returned AppMaps
     * @param maxSize    Maximum number of AppMaps to return
     * @return List of AppMaps with up to maxSize items of the current project, which match the given name filter.
     */
    @RequiresReadLock
    public @NotNull List<AppMapMetadata> findAppMaps(@Nullable String nameFilter, int maxSize) {
        if (DumbService.isDumb(project)) {
            return Collections.emptyList();
        }

        var lowercaseNameFilter = nameFilter == null ? null : nameFilter.toLowerCase();

        var appMapDirectories = AppMapNameIndex.getAppMapMetadataDirectories(project);
        var result = new ArrayList<AppMapMetadata>(appMapDirectories.size());
        for (var directory : appMapDirectories) {
            var data = AppMapNameIndex.getBasicMetadata(project, directory);
            if (data != null && data.getName() != null) {
                if (lowercaseNameFilter == null || data.getName().toLowerCase().contains(lowercaseNameFilter)) {
                    result.add(createMetadataWithIndex(directory,
                            data.getName(),
                            data.testStatus,
                            data.languageName, data.recorderType,
                            data.recorderName
                    ));
                }
            }
            if (result.size() >= maxSize) {
                break;
            }
        }
        return result;
    }

    /**
     * @param appMapFile .appmap.json file
     * @return The AppMap metadata for the given AppMap file.
     */
    public @Nullable AppMapMetadata getAppMapMetadata(@NotNull VirtualFile appMapFile) {
        var directory = AppMapFiles.findAppMapMetadataDirectory(appMapFile);
        return directory != null ? createMetadataWithIndex(directory) : null;
    }

    private @Nullable AppMapMetadata createMetadataWithIndex(@NotNull VirtualFile appMapMetadataDirectory) {
        var data = AppMapNameIndex.getBasicMetadata(project, appMapMetadataDirectory);
        if (data == null) {
            return null;
        }

        var name = data.name;
        if (name == null) {
            return null;
        }

        return createMetadataWithIndex(appMapMetadataDirectory,
                name,
                data.testStatus,
                data.languageName,
                data.recorderType,
                data.recorderName
        );
    }

    private @NotNull AppMapMetadata createMetadataWithIndex(@NotNull VirtualFile directory,
                                                            @NotNull String name,
                                                            @Nullable TestStatus testStatus,
                                                            @Nullable String languageName, @Nullable String recorderType,
                                                            @Nullable String recorderName) {
        var appMapFile = AppMapFiles.findAppMapByMetadataDirectory(directory);
        var requestCount = AppMapServerRequestCountIndex.getRequestCount(project, directory);
        var queryCount = AppMapSqlQueriesCountIndex.getQueryCount(project, directory);
        var functionsCount = ClassMapTypeIndex.findItemsByAppMapDirectory(project, directory, ClassMapItemType.Function).size();

        return new AppMapMetadata(name,
                appMapFile,
                testStatus,
                languageName,
                recorderType,
                recorderName,
                requestCount,
                queryCount,
                functionsCount);
    }
}
