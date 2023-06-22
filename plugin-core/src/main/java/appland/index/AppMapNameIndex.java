package appland.index;

import appland.utils.GsonUtils;
import com.google.gson.annotations.SerializedName;
import com.intellij.json.JsonFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.CommonProcessors;
import com.intellij.util.Processors;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.indexing.ID;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Indexes metadata.json files to extract the name property.
 */
public class AppMapNameIndex extends AbstractAppMapMetadataFileIndex<String> {
    private static final ID<Integer, String> INDEX_ID = ID.create("appmap.metadataFile");
    private static final String FILENAME = "metadata.json";

    /**
     * @param project Current project
     * @return The directories, based on this index, which contain AppMap metadata files.
     */
    public static @NotNull Set<VirtualFile> getAppMapMetadataDirectories(@NotNull Project project) {
        var scope = GlobalSearchScope.getScopeRestrictedByFileTypes(AppMapSearchScopes.projectFilesWithExcluded(project), JsonFileType.INSTANCE);
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
    public static @Nullable String getName(@NotNull Project project, @NotNull VirtualFile appMapMetadataDirectory) {
        return IndexUtil.getSingleEntryAppMapData(INDEX_ID, project, appMapMetadataDirectory, FILENAME);
    }

    /**
     * @param scope Search scope
     * @return {@code true} if there's no indexed AppMap in the given scope
     */
    public static boolean isEmpty(@NotNull GlobalSearchScope scope) {
        return FileBasedIndex.getInstance().processAllKeys(INDEX_ID, CommonProcessors.alwaysFalse(), scope, null);
    }

    @Override
    public @NotNull ID<Integer, String> getName() {
        return INDEX_ID;
    }

    @Override
    protected @NotNull String getIndexedFileName() {
        return FILENAME;
    }

    @Override
    protected @Nullable String parseMetadataFile(@NotNull String fileContent) {
        if (!fileContent.isEmpty()) {
            var json = GsonUtils.GSON.fromJson(fileContent, MetadataFileContent.class);
            if (json != null) {
                return json.name;
            }
        }
        return null;
    }

    @Override
    public @NotNull DataExternalizer<String> getValueExternalizer() {
        return EnumeratorStringDescriptor.INSTANCE;
    }

    private static class MetadataFileContent {
        @SerializedName("name")
        public String name;
    }
}
