package appland.files;

import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class AppMapVfsUtils {
    private AppMapVfsUtils() {
    }

    public static @NotNull Path asNativePath(@NotNull VirtualFile file) {
        var path = file.getFileSystem().getNioPath(file);
        return path != null ? path : Paths.get(FileUtilRt.toSystemDependentName(file.getPath()));
    }
}
