package appland.index;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.indexing.IndexableSetContributor;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Set;

public class AppMapIndexedRootsSetContributor extends IndexableSetContributor {
    @Override
    public @NonNls @NotNull String getDebugName() {
        return "AppMap directories";
    }

    @Override
    public @NotNull Set<VirtualFile> getAdditionalRootsToIndex() {
        return Collections.emptySet();
    }

    @Override
    public @NotNull Set<VirtualFile> getAdditionalProjectRootsToIndex(@NotNull Project project) {
        // in 2023.2, getAdditionalProjectRootsToIndex is executed in a NonBlockingReadAction.
        // in 2023.3, the ReadAction doesn't seem always available.
        return ApplicationManager.getApplication().isReadAccessAllowed()
                ? IndexUtil.findAppMapIndexDirectories(project)
                : ReadAction.compute(() -> IndexUtil.findAppMapIndexDirectories(project));
    }
}
