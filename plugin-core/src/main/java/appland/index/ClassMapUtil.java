package appland.index;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;

final class ClassMapUtil {
    static final String CLASS_MAP_FILE = "classMap.json";

    private ClassMapUtil() {
    }

    static boolean isClassMap(@Nullable VirtualFile file) {
        return file != null && CLASS_MAP_FILE.equals(file.getName());
    }
}
