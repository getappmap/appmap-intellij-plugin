package appland.execution;

import appland.config.AppMapConfigFile;
import appland.files.AppMapFiles;
import appland.index.AppMapSearchScopes;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.SearchScopeProvidingRunProfile;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScopesCore;
import com.intellij.util.PathUtil;
import com.intellij.util.concurrency.annotations.RequiresNoReadLock;
import com.intellij.util.concurrency.annotations.RequiresReadLock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates the content for an appmap.yml file, based on the current project.
 */
@SuppressWarnings("UnstableApiUsage")
public final class AppMapJavaPackageConfig {
    private AppMapJavaPackageConfig() {
    }

    /**
     * Attempts to locate a suitable appmap.yml file and creates a new one if none could be found.
     * If an existing appmap.yml does not contain an appmap_dir property, then the file is updated.
     *
     * @param module     Current module
     * @param runProfile Run profile to be executed
     * @param context    Context to help locate the parent directory for a newly create appmap.yml
     * @return The path to the appmap.yml file to pass to the AppMap agent. {@code null} if no file exists and creating the new file failed.
     */
    @RequiresNoReadLock
    public static @NotNull Path createOrUpdateAppMapConfig(@NotNull Module module,
                                                           @NotNull RunProfile runProfile,
                                                           @NotNull VirtualFile context,
                                                           @NotNull Path appMapOutputDirectory) throws IOException {

        // scope to locate existing appmap.yml
        var appMapConfigSearchScope = getAppMapConfigSearchScope(module, runProfile);

        // attempt to find an existing appmap.yml file
        // fixme lookup only in module (and dependencies?)
        var existingConfig = findAppMapConfig(module.getProject(), appMapConfigSearchScope);

        if (existingConfig != null) {
            var relativeOutputPath = existingConfig.getParent().toNioPath().relativize(appMapOutputDirectory);
            var configNioPath = existingConfig.toNioPath();
            updateAppMapConfig(configNioPath, relativeOutputPath);
            return configNioPath;
        }

        var configParentDirectory = AppMapJavaConfigUtil.findBestAppMapContentRootDirectory(module, context);
        return createAppMapConfig(module, configParentDirectory, appMapOutputDirectory);
    }

    @RequiresNoReadLock
    public static @NotNull Path createAppMapConfig(@NotNull Module module,
                                                   @NotNull VirtualFile configParentDirectory,
                                                   @NotNull Path appMapOutputDirectory) throws IOException {
        var configParentPath = configParentDirectory.toNioPath();
        if (appMapOutputDirectory.isAbsolute() && !appMapOutputDirectory.startsWith(configParentPath)) {
            throw new IllegalStateException(String.format("AppMap output directory is not inside the working directory: %s, %s",
                    configParentPath,
                    appMapOutputDirectory));
        }

        var appMapConfig = generateAppMapConfig(module, configParentPath.relativize(appMapOutputDirectory).toString());

        // create outside a read action, because JavaProgramPatcher is always called with a ReadAction
        // and we can't execute a WriteAction inside a read action
        // The only known workaround is to create the new configuration as an external file,
        // outside the VirtualFileSystem
        var appMapConfigPath = configParentPath.resolve(AppMapFiles.APPMAP_YML);
        appMapConfig.writeTo(appMapConfigPath);

        // must NOT be called in a read action to avoid a dead-lock, see JavaDoc of the method
        LocalFileSystem.getInstance().refreshAndFindFileByNioFile(appMapConfigPath);

        return appMapConfigPath;
    }

    /**
     * Updates property "appmap_dir" of an existing appmap.yml file.
     *
     * @param appMapConfig             The configuration to update
     * @param relativeAppMapOutputPath The new AppMap output path
     * @throws IOException Thrown if the file update failed
     */
    private static void updateAppMapConfig(@NotNull Path appMapConfig,
                                           @NotNull Path relativeAppMapOutputPath) throws IOException {
        var config = AppMapConfigFile.parseConfigFile(appMapConfig);
        if (config != null) {
            config.setAppMapDir(relativeAppMapOutputPath.toString());
            config.writeTo(appMapConfig);
        }
    }

    /**
     * @param module     Current module
     * @param runProfile Current run profile
     * @return The search scope to lookup appmap.yml, based on the working directory
     */
    private static @NotNull GlobalSearchScope getAppMapConfigSearchScope(@NotNull Module module, @Nullable RunProfile runProfile) {
        var scope = runProfile instanceof SearchScopeProvidingRunProfile
                ? ((SearchScopeProvidingRunProfile) runProfile).getSearchScope()
                : null;

        if (scope == null) {
            scope = AppMapSearchScopes.appMapConfigSearchScope(module.getProject());
        }

        scope = scope.intersectWith(module.getModuleScope(true));
        return scope;
    }

    // executed under progress
    private static @Nullable VirtualFile findAppMapConfig(@NotNull Project project,
                                                          @NotNull GlobalSearchScope runProfileScope) {
        return ReadAction.compute(() -> {
            var files = AppMapFiles.findAppMapConfigFiles(project, runProfileScope);
            return files.size() == 1 ? files.iterator().next() : null;
        });
    }

    /**
     * <pre>
     * # 'name' should generally be the same as the code repo name - or in IntelliJ, the project name
     * name: MyProject
     * packages:
     * - path: com.mycorp.myproject # Each configured source package can go here, sub-packages will be included automatically so don't list them individuallyn
     * </pre>
     *
     * @param appMapOutputPath Relative path value for the `appmap_dir` property, if available.
     *                         If this is {@code null} or empty, then the "build_dir" property will not be set.
     */
    @RequiresNoReadLock
    static AppMapConfigFile generateAppMapConfig(@NotNull Module module, @Nullable String appMapOutputPath) {
        // appmap_dir should be "dir/subdir" even on Windows
        var agnosticOutputPath = PathUtil.toSystemIndependentName(appMapOutputPath);

        var config = new AppMapConfigFile();
        config.setName(module.getProject().getName());
        config.setAppMapDir(agnosticOutputPath);
        config.setPackages(ReadAction.compute(() -> findTopLevelPackages(module)));
        return config;
    }

    /**
     * Top-level java packages located in the module or modules reachable from it (i.e. in dependency modules).
     * Only packages located in the current project are returned.
     *
     * @param module Starting point
     * @return List of Java package names
     */
    @NotNull
    @RequiresReadLock
    private static List<String> findTopLevelPackages(@NotNull Module module) {
        var roots = collectRootsWithDependencies(module);
        if (roots.length == 0) {
            return Collections.emptyList();
        }

        var sourceDirScope = GlobalSearchScopesCore.directoriesScope(module.getProject(), true, roots);
        var psiManager = PsiManager.getInstance(module.getProject());
        var result = new HashSet<String>();
        for (var sourceRoot : roots) {
            var directory = psiManager.findDirectory(sourceRoot);
            if (directory != null) {
                collectTopLevelPackages(directory, sourceDirScope, result);
            }
        }

        // sort for stable results
        return result.stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
    }

    private static void collectTopLevelPackages(@NotNull PsiDirectory directory,
                                                @NotNull GlobalSearchScope scope,
                                                @NotNull Collection<String> target) {
        var javaPackage = JavaDirectoryService.getInstance().getPackage(directory);
        if (javaPackage != null) {
            collectTopLevelPackages(javaPackage, scope, target);
        } else {
            for (var subdirectory : directory.getSubdirectories()) {
                collectTopLevelPackages(subdirectory, scope, target);
            }
        }
    }

    private static void collectTopLevelPackages(@NotNull PsiPackage javaPackage,
                                                @NotNull GlobalSearchScope scope,
                                                @NotNull Collection<String> target) {
        if (isEmptyPackage(javaPackage, scope)) {
            for (var subPackage : javaPackage.getSubPackages(scope)) {
                collectTopLevelPackages(subPackage, scope, target);
            }
        } else {
            target.add(javaPackage.getQualifiedName());
        }
    }

    private static boolean isEmptyPackage(@NotNull PsiPackage javaPackage, @NotNull GlobalSearchScope scope) {
        // default package
        if (javaPackage.getQualifiedName().isEmpty()) {
            return true;
        }

        // attempting to use streams and iterables based on the directory structure.
        // Calculating classes for a package is expensive for large packages.
        return Arrays.stream(javaPackage.getDirectories(scope)).allMatch(AppMapJavaPackageConfig::isEmptyPackageDir);
    }

    private static boolean isEmptyPackageDir(@NotNull PsiDirectory directory) {
        var child = directory.getFirstChild();
        while (child != null) {
            if (!(child instanceof PsiDirectory)) {
                return false;
            }
            child = child.getNextSibling();
        }
        return true;
    }

    @NotNull
    private static VirtualFile[] collectRootsWithDependencies(@NotNull Module module) {
        var allModules = new HashSet<Module>();
        allModules.add(module);
        allModules.addAll(Arrays.asList(ModuleRootManager.getInstance(module).getDependencies()));

        return allModules.stream()
                .flatMap(m -> Arrays.stream(ModuleRootManager.getInstance(m).getContentRoots()))
                .toArray(VirtualFile[]::new);
    }
}
