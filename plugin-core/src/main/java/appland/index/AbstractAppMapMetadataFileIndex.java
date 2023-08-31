package appland.index;

import com.google.gson.JsonParseException;
import com.intellij.json.JsonFileType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.indexing.FileContent;
import com.intellij.util.indexing.SingleEntryFileBasedIndexExtension;
import com.intellij.util.indexing.SingleEntryIndexer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Base class for an index based on a AppMap metadata file.
 * <p>
 * Only files in the local file system are indexed.
 *
 * @param <T> Type of the result value
 */
abstract class AbstractAppMapMetadataFileIndex<T> extends SingleEntryFileBasedIndexExtension<T> {
    private static final Logger LOG = Logger.getInstance(AbstractAppMapMetadataFileIndex.class);

    private final FileBasedIndex.InputFilter inputFilter = new NamedFileTypeFilter(JsonFileType.INSTANCE, fileName -> {
        return getIndexedFileName().equalsIgnoreCase(fileName);
    });

    private final SingleEntryIndexer<T> indexer = new SingleEntryIndexer<>(false) {
        @Override
        protected @Nullable T computeValue(@NotNull FileContent inputData) {
            try {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Indexing with " + getClass().getSimpleName() + ": " + inputData.getFile().getPath());
                }

                var value = parseMetadataFile(inputData.getContentAsText().toString());
                if (value == null && LOG.isDebugEnabled()) {
                    LOG.debug(String.format("Empty index result returned by AppMap metadata index: %s, file: %s",
                            getClass().getSimpleName(),
                            inputData.getFile().getPath()));
                }
                return value;
            } catch (JsonParseException e) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("error indexing AppMap metadata file: " + getIndexedFileName(), e);
                }
                return null;
            }
        }
    };

    /**
     * Parse the file content to retrieve the result.
     * {@link JsonParseException} thrown by this method are handled by the caller.
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
        return inputFilter;
    }

    @Override
    public final boolean dependsOnFileContent() {
        // final method to prevent an accidental override
        return true;
    }

    @Override
    public @NotNull SingleEntryIndexer<T> getIndexer() {
        return indexer;
    }
}
