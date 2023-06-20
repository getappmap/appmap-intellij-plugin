package appland.execution;

import appland.config.AppMapConfigFile;
import appland.files.AppMapFiles;
import appland.index.AppMapSearchScopes;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.SearchScopeProvidingRunProfile;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.progress.util.BackgroundTaskUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScopesCore;
import com.intellij.util.EmptyConsumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Generates the content for an appmap.yml file, based on the current project.
 */
public final class AppMapJavaPackageConfig {
    private AppMapJavaPackageConfig() {
    }

    /**
     * Attempts to locate a suitable appmap.yml file and creates a new one if none could be found.
     * If an existing appmap.yml does not contain an appmap_dir property, then the file is updated.
     *
     * @param project         Current project
     * @param runProfile      Run profile to be executed
     * @param appMapDirectory Path of the directory, where generated AppMaps files should be stored
     * @return The path to the appmap.yml file to pass to the AppMap agent. {@code null} if no file exists and creating the new file failed.
     */
    public static @NotNull Path createOrUpdateAppMapConfig(@NotNull Project project,
                                                           @NotNull RunProfile runProfile,
                                                           @NotNull VirtualFile workingDir,
                                                           @NotNull Path appMapDirectory) throws IOException {
        var workingDirPath = workingDir.toNioPath();
        if (!appMapDirectory.startsWith(workingDirPath)) {
            throw new IllegalStateException("AppMap output directory is not inside the working directory: " + workingDir + ", " + appMapDirectory);
        }

        var appMapConfigSearchScope = getAppMapConfigSearchScope(project, runProfile, workingDir);
        var relativeAppMapOutputPath = workingDirPath.relativize(appMapDirectory);

        // attempt to find an existing file
        var appMapConfig = BackgroundTaskUtil.computeInBackgroundAndTryWait(
                () -> findAppMapConfig(project, appMapConfigSearchScope),
                EmptyConsumer.getInstance(), TimeUnit.SECONDS.toMillis(15));

        if (appMapConfig != null) {
            var configNioPath = appMapConfig.toNioPath();
            updateAppMapConfig(configNioPath, relativeAppMapOutputPath);
            return configNioPath;
        }

        return createAppMapConfig(project,
                appMapConfigSearchScope,
                relativeAppMapOutputPath.toString(),
                workingDir.toNioPath());
    }

    private static @NotNull Path createAppMapConfig(@NotNull Project project,
                                                    @NotNull GlobalSearchScope configSearchScope,
                                                    @Nullable String relativeAppMapOutputPath,
                                                    @NotNull Path workingDirPath) throws IOException {

        var appMapConfig = BackgroundTaskUtil.computeInBackgroundAndTryWait(
                () -> generateAppMapConfig(project, configSearchScope, relativeAppMapOutputPath),
                EmptyConsumer.getInstance(),
                TimeUnit.SECONDS.toMillis(60));

        if (appMapConfig == null) {
            throw new IOException("Timeout creating a new AppMap configuration file");
        }

        // create outside a read action, because JavaProgramPatcher is always called with a ReadAction
        // and we can't execute a WriteAction inside a read action
        // The only known workaround is to create the new configuration as an external file,
        // outside the VirtualFileSystem
        var appMapConfigPath = workingDirPath.resolve(AppMapFiles.APPMAP_YML);
        appMapConfig.writeTo(appMapConfigPath);
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
     * @param project    Current project
     * @param runProfile Current run profile
     * @param workingDir Working directory
     * @return The search scope to lookup appmap.yml, based on the working directory
     */
    private static @NotNull GlobalSearchScope getAppMapConfigSearchScope(@NotNull Project project,
                                                                         @Nullable RunProfile runProfile,
                                                                         @Nullable VirtualFile workingDir) {
        var runProfileScope = runProfile instanceof SearchScopeProvidingRunProfile
                ? ((SearchScopeProvidingRunProfile) runProfile).getSearchScope()
                : null;

        if (runProfileScope == null) {
            runProfileScope = AppMapSearchScopes.projectFilesWithExcluded(project);
        }

        return workingDir == null
                ? runProfileScope
                : runProfileScope.uniteWith(new GlobalSearchScopesCore.DirectoryScope(project, workingDir, false));
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
    private static AppMapConfigFile generateAppMapConfig(@NotNull Project project,
                                                         @Nullable GlobalSearchScope runConfigurationScope,
                                                         @Nullable String appMapOutputPath) {
        var config = new AppMapConfigFile();
        config.setName(project.getName());
        config.setAppMapDir(appMapOutputPath);
        config.setPackages(ReadAction.compute(() -> findTopLevelPackages(project, runConfigurationScope)));
        return config;
    }

    @NotNull
    private static List<String> findTopLevelPackages(@NotNull Project project,
                                                     @Nullable GlobalSearchScope runConfigurationScope) {
        var roots = OrderEnumerator.orderEntries(project)
                .withoutLibraries().withoutSdk().withoutDepModules()
                .sources()
                .usingCache()
                .getRoots();

        if (roots.length == 0) {
            return Collections.emptyList();
        }

        var sourceDirScope = GlobalSearchScopesCore.directoriesScope(project, true, roots);
        var packageScope = runConfigurationScope != null
                ? sourceDirScope.intersectWith(runConfigurationScope)
                : sourceDirScope;

        var psiManager = PsiManager.getInstance(project);
        var result = new HashSet<String>();
        for (var sourceRoot : roots) {
            var directory = psiManager.findDirectory(sourceRoot);
            if (directory != null) {
                collectTopLevelPackages(directory, packageScope, result);
            }
        }
        // sort for stable results
        return result.stream().sorted(String.CASE_INSENSITIVE_ORDER).collect(Collectors.toList());
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
}
