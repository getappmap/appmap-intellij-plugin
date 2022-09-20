package appland.index;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.PathUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class AppMapFindingsUtil {
    private AppMapFindingsUtil() {
    }

    public static final String FINDINGS_FILE_NAME = "appmap-findings.json";

    public static boolean isFindingFile(@NotNull String path) {
        return FileUtil.fileNameEquals(PathUtil.getFileName(path), FINDINGS_FILE_NAME);
    }

    public static boolean isFindingFile(@Nullable VirtualFile file) {
        return file != null && FileUtil.fileNameEquals(file.getName(), FINDINGS_FILE_NAME);
    }
}
