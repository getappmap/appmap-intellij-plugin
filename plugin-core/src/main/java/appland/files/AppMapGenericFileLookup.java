package appland.files;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.PathUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * Generic fallback implementation for finding files in external libraries.
 * <p>
 * This lookup searches across all project scopes (including libraries) using filename
 * and relative path matching. It runs after language-specific lookups (order="last")
 * and supports any file type (Python, Ruby, JavaScript, etc.).
 * <p>
 * Uses {@link FilenameIndex} for performance and filters by relative path suffix to
 * minimize false matches.
 */
public class AppMapGenericFileLookup implements AppMapFileLookup {
    private static final Logger LOG = Logger.getInstance(AppMapGenericFileLookup.class);

    @Override
    public @Nullable VirtualFile findFile(@NotNull Project project, @NotNull Data data) {
        // Search by filename across all scopes (project + libraries)
        String filename = PathUtil.getFileName(data.relativePath()); // e.g., "Filter.java"
        Collection<VirtualFile> candidates = FilenameIndex.getVirtualFilesByName(
                filename, true, GlobalSearchScope.allScope(project)
        );

        // Filter to only files whose full path ends with the relative path
        // This is specific enough to avoid most false matches
        List<VirtualFile> matches = candidates.stream()
                .filter(file -> file.getPath().endsWith(data.relativePath()))
                .toList();

        if (matches.isEmpty()) return null;

        if (matches.size() > 1) {
            LOG.warn("Multiple candidates found for " + data.relativePath() +
                    ", using first match from: " + matches);
        }

        return matches.get(0);
    }
}
