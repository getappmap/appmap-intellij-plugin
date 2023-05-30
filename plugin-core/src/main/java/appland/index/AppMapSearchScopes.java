package appland.index;

import com.intellij.json.JsonFileType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;

public final class AppMapSearchScopes {
    private AppMapSearchScopes() {
    }

    public static @NotNull GlobalSearchScope projectFilesWithExcluded(@NotNull Project project) {
        return new EverythingExceptLibrariesScope(project);
    }

    /**
     * @param project Project
     * @return Search scope, which contains all .appmap.json files of the project, included files inside excluded directories. The scope is also restricted by the JSON file type.
     */
    public static @NotNull GlobalSearchScope appMapsWithExcluded(@NotNull Project project) {
        return GlobalSearchScope.getScopeRestrictedByFileTypes(projectFilesWithExcluded(project), JsonFileType.INSTANCE);
    }
}
