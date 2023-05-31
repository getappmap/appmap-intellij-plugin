package appland.index;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.concurrency.annotations.RequiresReadLock;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.indexing.ID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class IndexUtil {
    // base version for our indexes, increase when the data structures or the parsing logic change
    static final int BASE_VERSION = 56;

    @RequiresReadLock
    static <T> @Nullable T getSingleEntryAppMapData(@NotNull ID<Integer, T> indexId,
                                                    @NotNull Project project,
                                                    @NotNull VirtualFile appMapMetadataDirectory,
                                                    @NotNull String filename) {

        var indexedFile = appMapMetadataDirectory.findChild(filename);
        if (indexedFile == null) {
            return null;
        }

        return FileBasedIndex.getInstance().getSingleEntryIndexData(indexId, indexedFile, project);
    }
}
