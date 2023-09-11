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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public final class AppMapJavaConfigUtil {
    private AppMapJavaConfigUtil() {
    }

    /**
     * Locate the directory, where AppMap JSON files should be stored in the given module.
     * It always uses tmp/appmap as output directory. The returned tmp/appmap path is located in the most suitable
     * content root of the module.
     *
     * @param module  Current module
     * @param context Context to locate the most suitable content root for the AppMap output directory.
     * @return The path to the tmp/appmap directory, where AppMap files should be stored.
     */
    @RequiresReadLock
    public static @Nullable Path findAppMapOutputDirectory(@NotNull Module module, @NotNull VirtualFile context) {
        ApplicationManager.getApplication().assertReadAccessAllowed();

        var contentRoot = findBestAppMapContentRootDirectory(module, context);
        var contentRootPath = contentRoot.getFileSystem().getNioPath(contentRoot);
        return contentRootPath != null
                ? contentRootPath.resolve(Paths.get("tmp", "appmap"))
                : null;
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

        // content root of the module, which contains the context file
        var matchingModuleRoot = Arrays.stream(ModuleRootManager.getInstance(module).getContentRoots())
                .filter(root -> VfsUtilCore.isAncestor(root, context, false))
                .findFirst()
                .orElse(null);
        if (matchingModuleRoot != null) {
            return matchingModuleRoot;
        }

        // Some project models, e.g. Gradle, create multiple modules for a single gradle project.
        // For example, Gradle module "main" with content root "<root>/module-one/src/main"
        // and a top-level module with content root "<root>/module-one".
        // The context (e.g. "<root>/module-one") may be located outside the root of module (e.g "main").
        var projectContentRoot = ProjectRootManager.getInstance(module.getProject())
                .getFileIndex()
                .getContentRootForFile(context, false);
        if (projectContentRoot != null) {
            return projectContentRoot;
        }

        // fallback to the context itself for unexpected project setups
        return context.isDirectory()
                ? context
                : context.getParent();
    }
}
