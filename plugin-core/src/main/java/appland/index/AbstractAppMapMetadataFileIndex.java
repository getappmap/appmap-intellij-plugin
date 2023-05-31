package appland.index;

import com.google.gson.JsonParseException;
import com.intellij.json.JsonFileType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.indexing.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Base class for an indexe based on a AppMap metadata file.
 * <p>
 * Only files in the local file system are indexed.
 *
 * @param <T> Type of the result value
 */
abstract class AbstractAppMapMetadataFileIndex<T> extends SingleEntryFileBasedIndexExtension<T> {
    /**
     * Parse the file content to retrieve the result
     *
     * @param fileContent File content, metadata files are usually JSON
     * @return The result based on fileContent, if available.
     */
    protected abstract @Nullable T parseMetadataFile(@NotNull String fileContent);

    /**
     * @return The name of the indexed metadata file. This is needed to speed up the indexing by restricting it to this file only.
     */
    protected abstract @NotNull String getIndexedFileName();

    @Override
    public int getVersion() {
        return IndexUtil.BASE_VERSION;
    }

    @Override
    public @NotNull FileBasedIndex.InputFilter getInputFilter() {
        return new DefaultFileTypeSpecificInputFilter(JsonFileType.INSTANCE) {
            @Override
            public boolean acceptInput(@NotNull VirtualFile file) {
                return file.isInLocalFileSystem() && FileUtil.namesEqual(getIndexedFileName(), file.getName());
            }
        };
    }

    @Override
    public @NotNull SingleEntryIndexer<T> getIndexer() {
        return new SingleEntryIndexer<>(false) {
            @Override
            protected @Nullable T computeValue(@NotNull FileContent inputData) {
                try {
                    return parseMetadataFile(inputData.getContentAsText().toString());
                } catch (JsonParseException e) {
                    Logger.getInstance(SingleEntryIndexer.class).debug("error indexing AppMap metadata file: " + getIndexedFileName(), e);
                    return null;
                }
            }
        };
    }
}
