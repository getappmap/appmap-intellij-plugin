package appland.index;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Consumer;
import com.intellij.util.indexing.FileBasedIndex;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

class NamedFileTypeFilter implements FileBasedIndex.FileTypeSpecificInputFilter {
    private final @NotNull FileType fileType;
    private final Predicate<String> fileNameFilter;

    NamedFileTypeFilter(@NotNull FileType fileType, @NotNull Predicate<String> fileNameFilter) {
        this.fileType = fileType;
        this.fileNameFilter = fileNameFilter;
    }

    @Override
    public void registerFileTypesUsedForIndexing(@NotNull Consumer<? super FileType> fileTypeSink) {
        fileTypeSink.consume(fileType);
    }

    @Override
    public boolean acceptInput(@NotNull VirtualFile file) {
        return fileNameFilter.test(file.getName());
    }
}
