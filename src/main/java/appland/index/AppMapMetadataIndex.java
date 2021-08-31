package appland.index;

import com.intellij.json.JsonFileType;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.ProjectScope;
import com.intellij.util.Consumer;
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
import java.util.Collections;
import java.util.List;

/**
 * Index for appmap.json files of a project.
 */
public class AppMapMetadataIndex extends SingleEntryFileBasedIndexExtension<AppMapMetadata> {
    private static final ID<Integer, AppMapMetadata> INDEX_ID = ID.create("appmap.titleIndex");
    private static final FileBasedIndex.FileTypeSpecificInputFilter INPUT_FILTER = new FileBasedIndex.FileTypeSpecificInputFilter() {
        @Override
        public void registerFileTypesUsedForIndexing(@NotNull Consumer<? super FileType> fileTypeSink) {
            fileTypeSink.consume(JsonFileType.INSTANCE);
        }

        @Override
        public boolean acceptInput(@NotNull VirtualFile file) {
            return file.getName().endsWith(".appmap.json");
        }
    };

    private static final DataExternalizer<AppMapMetadata> dataExternalizer = new DataExternalizer<>() {
        @Override
        public void save(@NotNull DataOutput out, AppMapMetadata value) throws IOException {
            IOUtil.writeUTF(out, value.getName());
            IOUtil.writeUTF(out, value.getSystemIndependentFilepath());
            out.writeInt(value.getRequestCount());
            out.writeInt(value.getQueryCount());
            out.writeInt(value.getFunctionsCount());
        }

        @Override
        public AppMapMetadata read(@NotNull DataInput in) throws IOException {
            var name = IOUtil.readUTF(in);
            var filepath = IOUtil.readUTF(in);
            var requestCount = in.readInt();
            var queryCount = in.readInt();
            var functionsCount = in.readInt();
            return new AppMapMetadata(name, filepath, requestCount, queryCount, functionsCount);
        }
    };

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

        var index = FileBasedIndex.getInstance();
        var keys = index.getAllKeys(INDEX_ID, project);
        if (keys.isEmpty()) {
            return Collections.emptyList();
        }

        var lowercaseNameFilter = nameFilter == null ? null : nameFilter.toLowerCase();
        var scope = ProjectScope.getEverythingScope(project);
        var result = new ArrayList<AppMapMetadata>();
        for (var key : keys) {
            index.processValues(INDEX_ID, key, null, (file, value) -> {
                if (nameFilter == null || value.getName().toLowerCase().contains(lowercaseNameFilter)) {
                    result.add(value);
                }
                return result.size() < maxSize;
            }, scope);

            if (result.size() >= maxSize) {
                break;
            }
        }
        return result;
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
        return 8;
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
