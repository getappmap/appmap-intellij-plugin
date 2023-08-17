package appland.index;

import appland.problemsView.model.TestStatus;
import appland.utils.GsonUtils;
import com.google.gson.annotations.SerializedName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.Processors;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.indexing.ID;
import com.intellij.util.io.DataExternalizer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Indexes metadata.json files to extract the name property.
 */
public class AppMapNameIndex extends AbstractAppMapMetadataFileIndex<BasicAppMapMetadata> {
    private static final ID<Integer, BasicAppMapMetadata> INDEX_ID = ID.create("appmap.metadataFile");
    private static final String FILENAME = "metadata.json";

    /**
     * @param project Current project
     * @return The directories, based on this index, which contain AppMap metadata files.
     */
    public static @NotNull Set<VirtualFile> getAppMapMetadataDirectories(@NotNull Project project) {
        var scope = AppMapSearchScopes.appMapsWithExcluded(project);
        var keys = new HashSet<Integer>();
        FileBasedIndex.getInstance().processAllKeys(INDEX_ID, Processors.cancelableCollectProcessor(keys), scope, null);
        if (keys.isEmpty()) {
            return Collections.emptySet();
        }

        var directories = VfsUtilCore.createCompactVirtualFileSet();
        for (var key : keys) {
            for (var file : FileBasedIndex.getInstance().getContainingFiles(INDEX_ID, key, scope)) {
                directories.add(file.getParent());
            }
        }
        return directories;
    }

    /**
     * @param project                 Current project
     * @param appMapMetadataDirectory Metadata directory of an AppMap
     * @return The name of the AppMap, as defined by metadata.json
     */
    public static @Nullable String getName(@NotNull Project project,
                                           @NotNull VirtualFile appMapMetadataDirectory) {
        var data = IndexUtil.getSingleEntryAppMapData(INDEX_ID, project, appMapMetadataDirectory, FILENAME);
        return data != null ? data.name : null;
    }

    /**
     * @param project                 Current project
     * @param appMapMetadataDirectory Metadata directory of an AppMap
     * @return The name of the AppMap, as defined by metadata.json
     */
    public static @Nullable BasicAppMapMetadata getBasicMetadata(@NotNull Project project,
                                                                 @NotNull VirtualFile appMapMetadataDirectory) {
        return IndexUtil.getSingleEntryAppMapData(INDEX_ID, project, appMapMetadataDirectory, FILENAME);
    }

    /**
     * @param project Current project
     * @param scope   Search scope
     * @return {@code true} if there's no indexed AppMap in the given scope
     */
    public static boolean isEmpty(@NotNull Project project, @NotNull GlobalSearchScope scope) {
        var index = FileBasedIndex.getInstance();

        // we need to validate against the scope because the index keys may be outdated, e.g. of deleted files
        for (var key : index.getAllKeys(INDEX_ID, project)) {
            for (var file : index.getContainingFiles(INDEX_ID, key, scope)) {
                if (scope.contains(file)) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public @NotNull ID<Integer, BasicAppMapMetadata> getName() {
        return INDEX_ID;
    }

    @Override
    protected @NotNull String getIndexedFileName() {
        return FILENAME;
    }

    @Override
    protected @Nullable BasicAppMapMetadata parseMetadataFile(@NotNull String fileContent) {
        if (!fileContent.isEmpty()) {
            var json = GsonUtils.GSON.fromJson(fileContent, MetadataFileContent.class);
            if (json != null) {
                return new BasicAppMapMetadata(json.name, json.testStatus);
            }
        }
        return null;
    }

    @Override
    public @NotNull DataExternalizer<BasicAppMapMetadata> getValueExternalizer() {
        return BasicAppMapMetadataExternalizer.INSTANCE;
    }

    private static class MetadataFileContent {
        @SerializedName("name")
        public String name;

        @SerializedName("test_status")
        public @Nullable TestStatus testStatus = null;
    }
}
