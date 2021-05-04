package appland.index;

import com.intellij.json.JsonFileType;
import com.intellij.json.psi.JsonFile;
import com.intellij.json.psi.JsonObject;
import com.intellij.json.psi.JsonStringLiteral;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.ProjectScope;
import com.intellij.util.Consumer;
import com.intellij.util.indexing.*;
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
    private static final Logger LOG = Logger.getInstance("#appmap.index");
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
        }

        @Override
        public AppMapMetadata read(@NotNull DataInput in) throws IOException {
            var name = IOUtil.readUTF(in);
            var filepath = IOUtil.readUTF(in);
            return new AppMapMetadata(name, filepath);
        }
    };

    /**
     * Retrieves all AppMaps of a project from the index.
     *
     * @param project The current project
     * @return The list of AppMaps metadata objects.
     */
    public static @NotNull List<AppMapMetadata> findAppMaps(@NotNull Project project) {
        if (DumbService.isDumb(project)) {
            return Collections.emptyList();
        }

        var index = FileBasedIndex.getInstance();
        var keys = index.getAllKeys(INDEX_ID, project);
        if (keys.isEmpty()) {
            return Collections.emptyList();
        }

        var scope = ProjectScope.getAllScope(project);
        var result = new ArrayList<AppMapMetadata>(keys.size());
        for (var key : keys) {
            result.addAll(index.getValues(INDEX_ID, key, scope));
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
        return 5;
    }

    @Override
    public @NotNull DataExternalizer<AppMapMetadata> getValueExternalizer() {
        return dataExternalizer;
    }

    @Override
    public @NotNull SingleEntryIndexer<AppMapMetadata> getIndexer() {
        return new SingleEntryIndexer<>(false) {
            @Override
            protected @Nullable AppMapMetadata computeValue(@NotNull FileContent inputData) {
                var inputFile = inputData.getPsiFile();
                assert inputFile instanceof JsonFile;

                var jsonFile = (JsonFile) inputFile;
                var top = jsonFile.getTopLevelValue();
                if (!(top instanceof JsonObject)) {
                    LOG.debug("top property not an object");
                    return null;
                }

                var metadata = ((JsonObject) top).findProperty("metadata");
                if (metadata == null) {
                    LOG.debug("metadata property not found");
                    return null;
                }

                var metadataValue = metadata.getValue();
                if (!(metadataValue instanceof JsonObject)) {
                    LOG.debug("metadata property not an object");
                    return null;
                }

                var nameProperty = ((JsonObject) metadataValue).findProperty("name");
                if (nameProperty == null) {
                    LOG.debug("name metadata property not found");
                    return null;
                }

                var name = nameProperty.getValue();
                if (!(name instanceof JsonStringLiteral)) {
                    LOG.debug("name property is not a string");
                    return null;
                }

                return new AppMapMetadata(((JsonStringLiteral) name).getValue(), inputData.getFile().getPath());
            }
        };
    }
}
