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

        var moduleRoots = ModuleRootManager.getInstance(module).getContentRoots();
        var projectRoots = ProjectRootManager.getInstance(module.getProject()).getContentRoots();
        var candidateProjectRoot = Arrays.stream(projectRoots)
                .filter(contentRoot -> Arrays.stream(moduleRoots).anyMatch(moduleRoot -> {
                    return VfsUtilCore.isAncestor(contentRoot, moduleRoot, false);
                }))
                .min(Comparator.comparingInt(file -> file.getPath().length()));

        if (candidateProjectRoot.isPresent()) {
            return candidateProjectRoot.get();
        }

        // fallback to the context itself for unexpected project setups
        return context.isDirectory()
                ? context
                : context.getParent();
    }
}
