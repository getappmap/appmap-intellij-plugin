package appland.execution;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.concurrency.annotations.RequiresReadLock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.java.JavaModuleSourceRootTypes;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Collectors;

public final class AppMapJavaConfigUtil {
    private AppMapJavaConfigUtil() {
    }

    /**
     * Lookup the most suitable content root to store new AppMap JSON files.
     *
     * @param module  Current module
     * @param context Context for the lookup, doesn't have to be located inside module
     * @return Directory root to store AppMap JSON files, e.g. in subdir "tmp/appmap".
     */
    @RequiresReadLock
    public static @NotNull VirtualFile findBestAppMapContentRootDirectory(@NotNull Module module,
                                                                          @NotNull VirtualFile context) {
        ApplicationManager.getApplication().assertReadAccessAllowed();

        // Any of the module's roots will do
        var moduleRoot = ModuleRootManager.getInstance(module).getContentRoots()[0];
        var projectRoots = ProjectRootManager.getInstance(module.getProject()).getContentRoots();
        var candidateProjectRoots = Arrays.stream(projectRoots)
                .filter(sourceRoot -> VfsUtilCore.isAncestor(sourceRoot, moduleRoot, true))
                .sorted(Comparator.comparing(VirtualFile::toString))
                .toArray(VirtualFile[]::new);

        if (candidateProjectRoots.length > 0) {
            // The candidate roots are sorted, so  the top-level one will be
            // first (because it will be shortest).
            return candidateProjectRoots[0];
        }

        // fallback to the context itself for unexpected project setups
        return context.isDirectory()
                ? context
                : context.getParent();
    }
}
