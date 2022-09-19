package appland.execution;

import appland.files.AppMapFiles;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.SearchScopeProvidingRunProfile;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.util.BackgroundTaskUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScopesCore;
import com.intellij.util.EmptyConsumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
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
    public static @Nullable VirtualFile findOrCreateAppMapConfig(@NotNull Project project,
                                                                 @Nullable RunProfile runProfile,
                                                                 @Nullable VirtualFile workingDir) throws IOException {
        var runProfileScope = runProfile instanceof SearchScopeProvidingRunProfile
                ? ((SearchScopeProvidingRunProfile) runProfile).getSearchScope()
                : null;

        // attempt to find an existing file
        var appMapConfig = BackgroundTaskUtil.computeInBackgroundAndTryWait(
                () -> findAppMapConfig(project, runProfileScope),
                EmptyConsumer.getInstance(), TimeUnit.SECONDS.toMillis(15));

        if (appMapConfig != null) {
            return appMapConfig;
        }

        var newConfigContent = BackgroundTaskUtil.computeInBackgroundAndTryWait(
                () -> generateAppMapConfig(project, runProfileScope),
                EmptyConsumer.getInstance(),
                TimeUnit.SECONDS.toMillis(15));

        if (newConfigContent == null || workingDir == null) {
            return null;
        }

        // create a new config file
        var newConfigFile = new AtomicReference<VirtualFile>();
        ApplicationManager.getApplication().invokeAndWait(() -> {
            WriteAction.runAndWait(() -> {
                try {
                    var file = workingDir.findOrCreateChildData(AppMapJavaPackageConfig.class, AppMapFiles.APPMAP_YML);
                    VfsUtil.saveText(file, newConfigContent);
                    newConfigFile.set(file);
                } catch (Exception e) {
                    Logger.getInstance(AppMapJavaPackageConfig.class).warn("Unable to create new AppMap configuration file", e);
                }
            });
        }, ModalityState.defaultModalityState());
        return newConfigFile.get();
    }

    // executed under progress
    private static @Nullable VirtualFile findAppMapConfig(@NotNull Project project,
                                                          @Nullable GlobalSearchScope runProfileScope) {
        return ReadAction.compute(() -> {
            var scope = runProfileScope != null ? runProfileScope : GlobalSearchScope.everythingScope(project);
            var files = FilenameIndex.getFilesByName(project, AppMapFiles.APPMAP_YML, scope);
            return files.length == 1 ? files[0].getVirtualFile() : null;
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
