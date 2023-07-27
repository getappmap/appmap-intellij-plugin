package appland.installGuide;

import appland.execution.AppMapJavaConfigUtil;
import appland.execution.AppMapJavaPackageConfig;
import appland.files.AppMapFiles;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.JavaModuleType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.concurrency.annotations.RequiresReadLock;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Creates a new appmap.yml file in a Java project, when the install guide step "Install Agent" is executed.
 */
public class JavaInstallGuideListener implements InstallGuideListener {
    private static final Logger LOG = Logger.getInstance(JavaInstallGuideListener.class);

    private final Project project;

    public JavaInstallGuideListener(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public void afterInstallGuidePageOpened(@NotNull InstallGuideViewPage page) {
        if (page != InstallGuideViewPage.InstallAgent) {
            return;
        }

        var moduleWithRootForNewConfig = ReadAction.compute(() -> {
            return AppMapFiles.findAppMapConfigFiles(project).isEmpty()
                    ? findTargetModuleWithRoot()
                    : null;
        });

        // skip, if there already is an appmap.yml file in the project
        if (moduleWithRootForNewConfig == null) {
            return;
        }

        var module = moduleWithRootForNewConfig.first;
        var contentRoot = moduleWithRootForNewConfig.second;
        try {
            var appMapOutputDir = ReadAction.compute(() -> AppMapJavaConfigUtil.findAppMapOutputDirectory(module, contentRoot));
            if (appMapOutputDir != null) {
                AppMapJavaPackageConfig.createAppMapConfig(module,
                        contentRoot,
                        contentRoot.toNioPath());
            }
        } catch (Exception e) {
            LOG.debug("error creating new appmap configuration", e);
        }
    }

    /**
     * The click on "Install AppMap Agent" does not have context, therefore we're unable to handle Java projects
     * with more than one module, unless there's a module with a content root, which contains all other module's content roots.
     * This kind of setup is common for projects based on Gradle.
     */
    @RequiresReadLock
    private Pair<Module, VirtualFile> findTargetModuleWithRoot() {
        var javaModules = Arrays.stream(ModuleManager.getInstance(project).getModules())
                .filter(module -> ModuleType.is(module, JavaModuleType.getModuleType()))
                .collect(Collectors.toList());
        if (javaModules.isEmpty()) {
            return null;
        }

        if (javaModules.size() == 1) {
            var contentRoots = ModuleRootManager.getInstance(javaModules.get(0)).getContentRoots();
            return contentRoots.length == 1
                    ? Pair.create(javaModules.get(0), contentRoots[0])
                    : null;
        }

        var moduleToContentRoots = javaModules
                .stream()
                .collect(Collectors.toMap(Function.identity(), module -> Arrays.asList(ModuleRootManager.getInstance(module).getContentRoots())));

        var shortestRoot = moduleToContentRoots.values().stream()
                .flatMap(Collection::stream)
                .min(Comparator.comparing(VirtualFile::getPath))
                .orElse(null);
        if (shortestRoot == null) {
            return null;
        }

        return moduleToContentRoots.entrySet()
                .stream()
                .filter(entry -> entry.getValue().contains(shortestRoot))
                .findFirst()
                .map(entry -> Pair.create(entry.getKey(), shortestRoot))
                .orElse(null);
    }
}
