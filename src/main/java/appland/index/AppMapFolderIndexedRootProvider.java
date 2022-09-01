package appland.index;

import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.SmartHashSet;
import com.intellij.util.indexing.IndexableSetContributor;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Set;

/**
 * Adds folders named "appmap", which reside in excluded folders, to the indexing.
 */
public class AppMapFolderIndexedRootProvider extends IndexableSetContributor {
    @Override
    public @NotNull Set<VirtualFile> getAdditionalProjectRootsToIndex(@NotNull Project project) {
        var excludedAppmapFolders = new SmartHashSet<VirtualFile>();
        for (var module : ModuleManager.getInstance(project).getModules()) {
            for (var excludeRoot : ModuleRootManager.getInstance(module).getExcludeRoots()) {
                var appmapFolder = excludeRoot.findChild("appmap");
                if (appmapFolder != null) {
                    excludedAppmapFolders.add(appmapFolder);
                }
            }
        }
        return excludedAppmapFolders;
    }

    @Override
    public @NotNull Set<VirtualFile> getAdditionalRootsToIndex() {
        return Collections.emptySet();
    }
}
