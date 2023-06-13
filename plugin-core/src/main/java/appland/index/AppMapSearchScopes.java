package appland.index;

import com.intellij.json.JsonFileType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.FileIndexFacade;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.ProjectAwareVirtualFile;
import com.intellij.psi.search.ProjectScopeImpl;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class AppMapSearchScopes {
    private AppMapSearchScopes() {
    }

    public static @NotNull GlobalSearchScope projectFilesWithExcluded(@NotNull Project project) {
        return GlobalSearchScope.everythingScope(project);
    }

    /**
     * @param project Project
     * @return Search scope, which contains all .appmap.json files of the project, included files inside excluded directories. The scope is also restricted by the JSON file type.
     */
    public static @NotNull GlobalSearchScope appMapsWithExcluded(@NotNull Project project) {
        return GlobalSearchScope.getScopeRestrictedByFileTypes(projectFilesWithExcluded(project), JsonFileType.INSTANCE);
    }

    /**
     * Search scope, which contains all project files and also excluded files of the project.
     * This implementation is based on IntelliJ's {@link ProjectScopeImpl}.
     * <p>
     * We can't use {@code  GlobalSearchScope.allScope()} because it does not include excluded files of a project.
     * We can't use {@code  GlobalSearchScope.everythingScope()} because some implementations may contain more than the project's files.
     */
    private static final class ProjectWithExcludedScope extends GlobalSearchScope {
        private final FileIndexFacade fileIndex;

        public ProjectWithExcludedScope(@NotNull Project project) {
            super(project);
            fileIndex = FileIndexFacade.getInstance(project);
        }

        @Override
        public boolean contains(@NotNull VirtualFile file) {
            if (file instanceof ProjectAwareVirtualFile) {
                if (((ProjectAwareVirtualFile) file).isInProject(Objects.requireNonNull(getProject()))) {
                    return true;
                }
            }

            return file.isValid() && fileIndex.isExcludedFile(file) || fileIndex.isInProjectScope(file);
        }

        @Override
        public boolean isSearchInModuleContent(@NotNull Module aModule) {
            return true;
        }

        @Override
        public boolean isSearchInLibraries() {
            return false;
        }

        @NotNull
        @Override
        public String getDisplayName() {
            return "AppMap project with excluded";
        }

        @Override
        public String toString() {
            return getDisplayName();
        }

        @NotNull
        @Override
        public GlobalSearchScope uniteWith(@NotNull GlobalSearchScope scope) {
            if (scope == this) {
                return this;
            }
            return super.uniteWith(scope);
        }

        @NotNull
        @Override
        public GlobalSearchScope intersectWith(@NotNull GlobalSearchScope scope) {
            if (scope == this) {
                return this;
            }
            return super.intersectWith(scope);
        }
    }
}
