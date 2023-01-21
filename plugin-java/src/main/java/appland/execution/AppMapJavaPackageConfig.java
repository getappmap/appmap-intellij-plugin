package appland.execution;

import appland.files.AppMapFiles;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
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
     *
     * @param project    Current project
     * @param runProfile Run profile to be executed
     * @return The path to the appmap.yml file to pass to the AppMap agent. {@code null} if no file exists and creating the new file failed.
     */
    public static @Nullable Path findOrCreateAppMapConfig(@NotNull Project project,
                                                          @Nullable RunProfile runProfile,
                                                          @Nullable VirtualFile workingDir) throws IOException {
        var runProfileScope = runProfile instanceof SearchScopeProvidingRunProfile
                ? ((SearchScopeProvidingRunProfile) runProfile).getSearchScope()
                : null;
        if (runProfileScope == null) {
            runProfileScope = GlobalSearchScope.everythingScope(project);
        }
        var runProfileAndWorkingDir = workingDir == null
                ? runProfileScope
                : runProfileScope.uniteWith(new GlobalSearchScopesCore.DirectoryScope(project, workingDir, false));

        // attempt to find an existing file
        var appMapConfig = BackgroundTaskUtil.computeInBackgroundAndTryWait(
                () -> findAppMapConfig(project, runProfileAndWorkingDir),
                EmptyConsumer.getInstance(), TimeUnit.SECONDS.toMillis(15));

        if (appMapConfig != null) {
            return appMapConfig.toNioPath();
        }

        var newConfigContent = BackgroundTaskUtil.computeInBackgroundAndTryWait(
                () -> generateAppMapConfig(project, runProfileAndWorkingDir),
                EmptyConsumer.getInstance(),
                TimeUnit.SECONDS.toMillis(15));

        if (newConfigContent == null || workingDir == null) {
            return null;
        }

        // create outside a read action, because JavaProgramPatcher is always called with a ReadAction
        // and we can't execute a WriteAction inside a read action
        // The only known workaround is to create the new configuration as an external file,
        // outside the VirtualFileSystem
        var workingDirPath = workingDir.toNioPath();
        var appMapConfigPath = workingDirPath.resolve(AppMapFiles.APPMAP_YML);
        Files.write(appMapConfigPath, newConfigContent.getBytes(StandardCharsets.UTF_8));
        return appMapConfigPath;
    }

    // executed under progress
    private static @Nullable VirtualFile findAppMapConfig(@NotNull Project project,
                                                          @NotNull GlobalSearchScope runProfileScope) {
        return ReadAction.compute(() -> {
            var files = AppMapFiles.findAppMapConfigFiles(project, runProfileScope);
            return files.size() == 1 ? files.iterator().next() : null;
        });
    }

    /*
        # 'name' should generally be the same as the code repo name - or in IntelliJ, the project name
        name: MyProject
        packages:
        - path: com.mycorp.myproject # Each configured source package can go here, sub-packages will be included automatically so don't list them individually
     */
    public static String generateAppMapConfig(@NotNull Project project,
                                              @Nullable GlobalSearchScope runConfigurationScope) {
        var config = new StringBuilder();
        config.append("name: ").append(project.getName()).append('\n');
        config.append("packages:").append('\n');

        for (var packageName : ReadAction.compute(() -> findTopLevelPackages(project, runConfigurationScope))) {
            config.append("- path: ").append(packageName).append('\n');
        }

        return config.toString();
    }

    @NotNull
    private static Collection<String> findTopLevelPackages(@NotNull Project project,
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
