package appland.files;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public final class AppMapFiles {
    private AppMapFiles() {
    }

    /**
     * @return {@code true} if the given file is an AppMap.
     */
    public static boolean isAppMap(@NotNull VirtualFile file) {
        return file.isValid() && !file.isDirectory() && file.getName().endsWith(".appmap.json");
    }
}
