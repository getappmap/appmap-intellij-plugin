package appland.execution;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.CompilerPaths;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.concurrency.annotations.RequiresReadLock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;

public final class AppMapJavaConfigUtil {
    private AppMapJavaConfigUtil() {
    }

    // Supported top-level names of build output directories of Gradle and Maven.
    // We're not including IntelliJ's default of "out" because we're using a fallback of "tmp" instead.
    private static final Set<String> OUTPUT_TOP_LEVEL_NAMES = Set.of("target", "build");

    /**
     * Locate the directory, where AppMap JSON files should be stored in the given module.
     *
     * @param module  Current module
     * @param context Context, which is used to compute the fallback if no output directory is defined for the module.
     * @return The path to the directory, where AppMap files should be stored.
     */
    @RequiresReadLock
    public static @Nullable Path findAppMapOutputDirectory(@NotNull Module module, @NotNull VirtualFile context) {
        ApplicationManager.getApplication().assertReadAccessAllowed();

        var topLevelOutputDir = findSupportedTopLevelOutputDirectory(module);
        if (topLevelOutputDir != null) {
            var nioPath = topLevelOutputDir.getFileSystem().getNioPath(topLevelOutputDir);
            return nioPath != null
                    ? nioPath.resolve("appmap")
                    : null;
        }

        var contentRoot = findBestAppMapContentRootDirectory(module, context);
        var contentRootPath = contentRoot.getFileSystem().getNioPath(contentRoot);
        if (contentRootPath != null) {
            return contentRootPath.resolve("tmp").resolve("appmap");
        }

        return null;
    }

    @RequiresReadLock
    private static @Nullable VirtualFile findSupportedTopLevelOutputDirectory(@NotNull Module module) {
        // e.g. "<root>/build/java/classes/main" (Gradle) or "<root>/target/classes" (Maven)
        var outputDirectory = CompilerPaths.getModuleOutputDirectory(module, false);
        if (outputDirectory == null) {
            return null;
        }

        var dir = outputDirectory;
        while (dir != null) {
            if (OUTPUT_TOP_LEVEL_NAMES.contains(dir.getName())) {
                try {
                    // It's only valid if it's still located inside the project,
                    // a path like "/work/build" for output directory "/work/build/my-project/out/class" is invalid.
                    return isLocatedInProject(module, dir)
                            ? dir
                            : null;
                } catch (Exception e) {
                    return null;
                }
            }
            dir = dir.getParent();
        }

        return null;
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
        // The context (e.g. "<root>/module-one") may be located outside the root of module (.e.g "main").
        var projectContentRoot = ProjectRootManager.getInstance(module.getProject()).getFileIndex().getContentRootForFile(context, false);
        if (projectContentRoot != null) {
            return projectContentRoot;
        }

        // fallback to the context itself for unexpected project setups
        return context.isDirectory()
                ? context
                : context.getParent();
    }

    private static boolean isLocatedInProject(@NotNull Module module, @NotNull VirtualFile dir) {
        var index = ProjectRootManager.getInstance(module.getProject()).getFileIndex();
        return index.getContentRootForFile(dir, false) != null;
    }
}
