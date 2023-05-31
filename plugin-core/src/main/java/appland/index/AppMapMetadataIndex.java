package appland.index;

import com.intellij.json.JsonFileType;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.indexing.ID;
import com.intellij.util.indexing.SingleEntryFileBasedIndexExtension;
import com.intellij.util.indexing.SingleEntryIndexer;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.IOUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Index for appmap.json files of a project.
 */
public class AppMapMetadataIndex extends SingleEntryFileBasedIndexExtension<AppMapMetadata> {
    private static final ID<Integer, AppMapMetadata> INDEX_ID = ID.create("appmap.titleIndex");
    private static final FileBasedIndex.FileTypeSpecificInputFilter INPUT_FILTER = new NamedFileTypeFilter(JsonFileType.INSTANCE, name -> name.endsWith(".appmap.json"));

    private static final DataExternalizer<AppMapMetadata> dataExternalizer = new DataExternalizer<>() {
        @Override
        public void save(@NotNull DataOutput out, @NotNull AppMapMetadata value) throws IOException {
            IOUtil.writeUTF(out, value.getName());
            IOUtil.writeUTF(out, value.getSystemIndependentFilepath());
            out.writeInt(value.getRequestCount());
            out.writeInt(value.getQueryCount());
            out.writeInt(value.getFunctionsCount());
        }

        @Override
        public @NotNull AppMapMetadata read(@NotNull DataInput in) throws IOException {
            var name = IOUtil.readUTF(in);
            var filepath = IOUtil.readUTF(in);
            var requestCount = in.readInt();
            var queryCount = in.readInt();
            var functionsCount = in.readInt();
            return new AppMapMetadata(name, filepath, requestCount, queryCount, functionsCount);
        }
    };

    public static @Nullable AppMapMetadata findAppMap(@NotNull Project project, @NotNull VirtualFile file) {
        if (DumbService.isDumb(project)) {
            return null;
        }

        var fileData = FileBasedIndex.getInstance().getFileData(INDEX_ID, file, project);
        return fileData.isEmpty() ? null : fileData.values().iterator().next();
    }

    /**
     * Retrieves all AppMaps of a project from the index.
     *
     * @param project    The current project
     * @param nameFilter Optional filter to restrict items by name. The name is matched case-insensitive.
     * @return The list of AppMaps metadata objects.
     */
    public static @NotNull List<AppMapMetadata> findAppMaps(@NotNull Project project, @Nullable String nameFilter) {
        return findAppMaps(project, nameFilter, Integer.MAX_VALUE);
    }

    /**
     * Retrieves all AppMaps of a project from the index.
     *
     * @param project    The current project
     * @param nameFilter Optional filter to restrict items by name. The name is matched case-insensitive.
     * @param maxSize    The maximum number of items to add.
     * @return The list of AppMaps metadata objects.
     */
    public static @NotNull List<AppMapMetadata> findAppMaps(@NotNull Project project, @Nullable String nameFilter, int maxSize) {
        if (DumbService.isDumb(project)) {
            return Collections.emptyList();
        }

        var lowercaseNameFilter = nameFilter == null ? null : nameFilter.toLowerCase();
        var result = new ArrayList<AppMapMetadata>();
        processAppMaps(project, AppMapSearchScopes.appMapsWithExcluded(project), (file, value) -> {
            if (nameFilter == null || value.getName().toLowerCase().contains(lowercaseNameFilter)) {
                result.add(value);
            }
            return result.size() < maxSize;
        });
        return result;
    }

    public static void processAppMaps(@NotNull Project project,
                                      @NotNull GlobalSearchScope scope,
                                      @NotNull FileBasedIndex.ValueProcessor<AppMapMetadata> processor) {
        if (DumbService.isDumb(project)) {
            return;
        }

        var index = FileBasedIndex.getInstance();
        for (var key : index.getAllKeys(INDEX_ID, project)) {
            if (!index.processValues(INDEX_ID, key, null, processor, scope)) {
                break;
            }
        }
    }

    /**
     * Enforce indexing of files of all sizes.
     */
    @Override
    public @NotNull Collection<FileType> getFileTypesWithSizeLimitNotApplicable() {
        return Collections.singletonList(JsonFileType.INSTANCE);
    }

    @Override
    public @NotNull ID<Integer, AppMapMetadata> getName() {
        return INDEX_ID;
    }

    @Override
    public @NotNull FileBasedIndex.InputFilter getInputFilter() {
        return INPUT_FILTER;
    }

    @Override
    public int getVersion() {
        return IndexUtil.BASE_VERSION;
    }

    @Override
    public @NotNull DataExternalizer<AppMapMetadata> getValueExternalizer() {
        return dataExternalizer;
    }

    @Override
    public @NotNull SingleEntryIndexer<AppMapMetadata> getIndexer() {
        return new StreamingAppMapIndexer();
    }
}
