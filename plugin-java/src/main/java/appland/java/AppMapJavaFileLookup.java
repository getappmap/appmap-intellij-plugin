package appland.java;

import appland.files.AppMapFileLookup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Java-specific implementation for finding classes in external libraries (JARs).
 * <p>
 * Converts file paths to fully qualified class names and uses {@link JavaPsiFacade}
 * to locate classes. This automatically:
 * <ul>
 *   <li>Resolves to source attachments when available</li>
 *   <li>Falls back to decompiled view if no source is attached</li>
 *   <li>Handles inner classes and classpath resolution correctly</li>
 * </ul>
 * This is the same mechanism IntelliJ's "Go to Class" uses internally.
 */
public class AppMapJavaFileLookup implements AppMapFileLookup {
    @Override
    public @Nullable VirtualFile findFile(@NotNull Project project, @NotNull Data data) {
        String relativePath = data.relativePath();
        if (!relativePath.endsWith(".java")) {
            return null;
        }

        // Convert path to FQN: org/springframework/web/filter/Filter.java -> org.springframework.web.filter.Filter
        String fqn = relativePath.substring(0, relativePath.length() - ".java".length())
                .replace('/', '.')
                .replace('\\', '.');

        PsiClass psiClass = JavaPsiFacade.getInstance(project).findClass(fqn, GlobalSearchScope.allScope(project));
        if (psiClass == null) {
            return null;
        }

        return psiClass.getContainingFile().getVirtualFile();
    }
}
