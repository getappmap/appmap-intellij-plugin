package appland.index;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.WatchedRootsProvider;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public class AppMapWatchedRootsProvider implements WatchedRootsProvider {
    @Override
    public @NotNull Set<String> getRootsToWatch(@NotNull Project project) {
        // 2025.3 doesn't use a read action in unit test mode, but findAppMapIndexDirectories needs it.
        var application = ApplicationManager.getApplication();
        var indexDirectories = application.isUnitTestMode() && !application.isReadAccessAllowed()
                ? ReadAction.compute(() -> IndexUtil.findAppMapIndexDirectories(project))
                : IndexUtil.findAppMapIndexDirectories(project);

        var paths = new HashSet<String>();
        for (var directory : indexDirectories) {
            // VirtualDirectoryImpl.allChildrenLoaded() became internal API in 2025.3.
            // We're supposed to use it, but we are not allowed to

            if (directory.isDirectory()) {
                directory.getChildren();
            }

            paths.add(directory.getPath());
        }
        return paths;
    }
}
