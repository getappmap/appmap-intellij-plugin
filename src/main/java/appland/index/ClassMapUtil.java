package appland.index;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class ClassMapUtil {
    private ClassMapUtil() {
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
