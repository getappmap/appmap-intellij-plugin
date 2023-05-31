package appland.index;

import appland.utils.GsonUtils;
import com.google.gson.JsonArray;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.indexing.ID;
import com.intellij.util.io.DataExternalizer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Index of AppMap request counts, as defined by the AppMap metadata.
 */
public class AppMapServerRequestCountIndex extends AbstractAppMapMetadataFileIndex<Integer> {
    private static final ID<Integer, Integer> INDEX_ID = ID.create("appmap.serverRequests");
    private static final String FILENAME = "canonical.httpServerRequests.json";

    /**
     * @param project                 Current project
     * @param appMapMetadataDirectory Metadata directory of an AppMap
     * @return The number of requests defined by the metadata
     */
    public static int getRequestCount(@NotNull Project project, @NotNull VirtualFile appMapMetadataDirectory) {
        var data = IndexUtil.getSingleEntryAppMapData(INDEX_ID, project, appMapMetadataDirectory, FILENAME);
        return data != null ? data : 0;
    }

    @Nullable
    @Override
    protected Integer parseMetadataFile(@NotNull String fileContent) {
        if (!fileContent.isEmpty()) {
            var json = GsonUtils.GSON.fromJson(fileContent, JsonArray.class);
            if (json != null) {
                return json.size();
            }
        }
        return 0;
    }

    @Override
    protected @NotNull String getIndexedFileName() {
        return FILENAME;
    }

    @Override
    public @NotNull ID<Integer, Integer> getName() {
        return INDEX_ID;
    }

    @Override
    public @NotNull DataExternalizer<Integer> getValueExternalizer() {
        return IntegerDataExternalizer.INSTANCE;
    }
}
