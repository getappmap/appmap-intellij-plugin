package appland.utils;

import appland.files.AppMapFiles;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.concurrency.annotations.RequiresReadLock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class AppMapProjectUtil {
    private AppMapProjectUtil() {
    }

    @RequiresReadLock
    public static @NotNull String getAppMapProjectName(@NotNull Project project, @Nullable VirtualFile contentFile) {
        ApplicationManager.getApplication().assertReadAccessAllowed();

        var root = contentFile != null
                ? AppMapFiles.findTopLevelContentRoot(project, contentFile)
                : null;
        return root != null
                ? root.getName()
                : "- unknown -";
    }
}
