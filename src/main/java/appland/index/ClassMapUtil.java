package appland.index;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class ClassMapUtil {
    static final String CLASS_MAP_FILE = "classMap.json";

    private ClassMapUtil() {
    }

    static boolean isClassMap(@Nullable VirtualFile file) {
        return file != null && CLASS_MAP_FILE.equals(file.getName());
    }

    static @Nullable VirtualFile findAppMapSourceFile(@NotNull VirtualFile classMapFile) {
        var parent = classMapFile.getParent();
        if (parent == null) {
            return null;
        }

        var appMapName = FileUtil.getNameWithoutExtension(parent.getName());
        return classMapFile.getParent().findFileByRelativePath("../" + appMapName + ".appmap.json");
    }
}
