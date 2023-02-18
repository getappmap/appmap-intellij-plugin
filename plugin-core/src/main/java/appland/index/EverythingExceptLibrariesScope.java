package appland.index;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFileFilter;
import com.intellij.psi.search.DelegatingGlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;

/**
 * We're using everythingScope because it contains excluded files, which are needed for the AppMap index data,
 * but it's also including libraries, and more unrelated files.
 */
class EverythingExceptLibrariesScope extends DelegatingGlobalSearchScope {
    EverythingExceptLibrariesScope(@NotNull Project project) {
        super(GlobalSearchScope.everythingScope(project));
    }

    @Override
    public @NotNull VirtualFileFilter and(@NotNull VirtualFileFilter other) {
        return super.and(other);
    }

    @Override
    public boolean isSearchInLibraries() {
        return false;
    }

    @Override
    public boolean isForceSearchingInLibrarySources() {
        return false;
    }
}
