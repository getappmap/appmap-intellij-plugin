package appland.index;

import appland.files.AppMapFiles;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.indexing.*;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

/**
 * Indexes appmap.yml configuration files to locate them quickly.
 */
public class AppMapConfigFileIndex extends ScalarIndexExtension<String> {
    public static final ID<String, Void> INDEX_ID = ID.create("appmap.configFile");

    @Override
    public @NotNull ID<String, Void> getName() {
        return INDEX_ID;
    }

    @Override
    public int getVersion() {
        return IndexUtil.BASE_VERSION;
    }

    @Override
    public @NotNull KeyDescriptor<String> getKeyDescriptor() {
        return EnumeratorStringDescriptor.INSTANCE;
    }

    @Override
    public boolean dependsOnFileContent() {
        return false;
    }

    @Override
    @NotNull
    public FileBasedIndex.InputFilter getInputFilter() {
        return VirtualFile::isInLocalFileSystem;
    }

    @Override
    public @NotNull DataIndexer<String, Void, FileContent> getIndexer() {
        return fileContent -> {
            var fileName = fileContent.getFileName();
            return AppMapFiles.isAppMapConfigFileName(fileName)
                    ? Collections.singletonMap(fileName, null)
                    : Collections.emptyMap();
        };
    }
}
