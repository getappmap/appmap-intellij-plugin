package appland.execution;

import appland.config.AppMapConfigFile;
import appland.files.AppMapFiles;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.text.Strings;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScopesCore;
import com.intellij.util.PathUtil;
import com.intellij.util.concurrency.annotations.RequiresReadLock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.java.JavaModuleSourceRootTypes;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages the content of appmap.yml files for Java projects.
 */
final class AppMapJavaPackageConfig {
    private AppMapJavaPackageConfig() {
    }

    /**
     * Locate an existing AppMap based on the given context and search scope.
     * If it exists, it adds the appmap_dir property with value "tmp/appmap" to the configuration if this property is not yet in the file.
     * This method must be called outside a read action.
     */
    public static @Nullable Path findAndUpdateAppMapConfig(@NotNull VirtualFile context,
                                                           @NotNull GlobalSearchScope searchScope) throws IOException {
        var configFile = ReadAction.compute(() -> AppMapFiles.findAppMapConfigFile(context, searchScope));
        if (configFile == null) {
            return null;
        }

        var configFilePath = configFile.toNioPath();
        AppMapJavaPackageConfig.addAppMapDirIfMissing(configFilePath, Path.of("tmp", "appmap"));
        return configFilePath;
    }

    public static @NotNull Path createAppMapConfig(@NotNull Module module,
                                                   @NotNull VirtualFile appMapContentRootDirectory,
                                                   @NotNull Path appMapOutputDirectory) throws IOException {
        assert (appMapContentRootDirectory.isDirectory());

        var configParentPath = appMapContentRootDirectory.toNioPath();
        if (appMapOutputDirectory.isAbsolute() && !appMapOutputDirectory.startsWith(configParentPath)) {
            throw new IllegalStateException(String.format("AppMap output directory is not inside the working directory: %s, %s",
                    configParentPath,
                    appMapOutputDirectory));
        }

        var relativeAppMapOutputPath = appMapOutputDirectory.isAbsolute()
                ? configParentPath.relativize(appMapOutputDirectory)
                : appMapOutputDirectory;
        var appMapConfig = generateAppMapConfig(module.getProject(), appMapContentRootDirectory, relativeAppMapOutputPath.toString());

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
    private static void addAppMapDirIfMissing(@NotNull Path appMapConfig,
                                              @NotNull Path relativeAppMapOutputPath) throws IOException {
        var config = AppMapConfigFile.parseConfigFile(appMapConfig);
        if (config != null && Strings.isEmpty(config.getAppMapDir())) {
            // appmap_dir should be "tmp/appmap" even on Windows
            var agnosticOutputPath = PathUtil.toSystemIndependentName(relativeAppMapOutputPath.toString());
            config.setAppMapDir(agnosticOutputPath);
            config.writeTo(appMapConfig);
        }
    }

    /**
     * <pre>
     * # 'name' should generally be the same as the code repo name - or in IntelliJ, the project name
     * name: MyProject
     * packages:
     * - path: com.mycorp.myproject # Each configured source package can go here, sub-packages will be included automatically so don't list them individuallyn
     * </pre>
     *
     * @param project                    Current project
     * @param appMapContentRootDirectory The AppMap content root directory, where appmap.yml is created
     * @param appMapOutputPath           Relative path value for the `appmap_dir` property, if available.
     *                                   If this is {@code null} or empty, then the "build_dir" property will not be set.
     */
    static AppMapConfigFile generateAppMapConfig(@NotNull Project project,
                                                 @NotNull VirtualFile appMapContentRootDirectory,
                                                 @Nullable String appMapOutputPath) {
        // appmap_dir should be "dir/subdir" even on Windows
        var agnosticOutputPath = PathUtil.toSystemIndependentName(appMapOutputPath);

        var config = new AppMapConfigFile();
        config.setName(project.getName());
        config.setAppMapDir(agnosticOutputPath);
        config.setPackages(ReadAction.compute(() -> findTopLevelPackages(project, appMapContentRootDirectory)));
        return config;
    }

    /**
     * Top-level java packages located in the module or modules reachable from it (i.e. in dependency modules).
     * Only packages located in the current project are returned.
     *
     * @param project                    Current project
     * @param appMapContentRootDirectory Selected AppMap content root directory
     * @return List of Java package names
     */
    @NotNull
    @RequiresReadLock
    private static List<String> findTopLevelPackages(@NotNull Project project,
                                                     @NotNull VirtualFile appMapContentRootDirectory) {
        var roots = collectSourceRootsWithDependencies(project, appMapContentRootDirectory);
        if (roots.length == 0) {
            return Collections.emptyList();
        }

        // directoriesScope allows to search in libraries, and we must not add library Java packages to appmap.yml
        var sourcesWithoutLibrariesScope = GlobalSearchScopesCore.directoriesScope(project, true, roots)
                .intersectWith(GlobalSearchScope.projectScope(project));

        var psiManager = PsiManager.getInstance(project);
        var result = new HashSet<String>();
        for (var sourceRoot : roots) {
            var directory = psiManager.findDirectory(sourceRoot);
            if (directory != null) {
                collectTopLevelPackages(directory, sourcesWithoutLibrariesScope, result);
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

    /**
     * @param project                    Current project
     * @param appMapContentRootDirectory AppMap content root directory
     * @return All source roots (sources and resources), which may contain packages for appmap.yml
     */
    @NotNull
    private static VirtualFile[] collectSourceRootsWithDependencies(@NotNull Project project,
                                                                    @NotNull VirtualFile appMapContentRootDirectory) {
        var projectRootManager = ProjectRootManager.getInstance(project);

        // All source roots of the project, which are below the AppMap content root may contain Java packages to be
        // included in the new appmap.yml file.
        // We need source roots to avoid loading packages from build output directories,
        // as seen with spring-petclinic classes at build/classes/java/aotTest.
        return projectRootManager
                .getModuleSourceRoots(JavaModuleSourceRootTypes.SOURCES)
                .stream()
                .filter(sourceRoot -> VfsUtilCore.isAncestor(appMapContentRootDirectory, sourceRoot, false))
                .toArray(VirtualFile[]::new);
    }
}
