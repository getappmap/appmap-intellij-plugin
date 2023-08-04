package appland.index;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.WatchedRootsProvider;
import com.intellij.openapi.vfs.newvfs.impl.VirtualDirectoryImpl;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public class AppMapWatchedRootsProvider implements WatchedRootsProvider {
    @Override
    public @NotNull Set<String> getRootsToWatch(@NotNull Project project) {
        var paths = new HashSet<String>();
        for (var directory : IndexUtil.findAppMapIndexDirectories(project)) {
            if (directory instanceof VirtualDirectoryImpl) {
                if (!((VirtualDirectoryImpl) directory).allChildrenLoaded()) {
                    directory.getChildren();
                }
            }
            paths.add(directory.getPath());
        }
        return paths;
    }
}
