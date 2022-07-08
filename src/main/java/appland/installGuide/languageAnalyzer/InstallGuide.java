package appland.installGuide.languageAnalyzer;

import com.google.gson.JsonElement;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

public class InstallGuide {
    public static JsonElement scanProjectAsyncTree(@NotNull Project project) {
        return ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
            return ReadAction.compute(() -> {
                var projects = scanProject(project);
                return GsonUtils.GSON.toJsonTree(projects);
            });
        }, "Scanning project", true, project);
    }

    public static List<Result> scanProject(@NotNull Project project) {
        var projects = new ArrayList<Result>();
        var resolver = new LanguageResolver();

        for (var root : findTopLevelContentRoots(project)) {
            var language = resolver.getLanguage(root);
            if (language != null) {
                var analyzer = LanguageAnalyzers.create(language);
                if (analyzer != null) {
                    var analyze = analyzer.analyze(root);
                    projects.add(analyze);
                }
            }
        }
        return projects;
    }

    @NotNull
    private static VirtualFile[] findTopLevelContentRoots(@NotNull Project project) {
        var roots = new ArrayList<>(List.of(ProjectRootManager.getInstance(project).getContentRoots()));
        roots.sort(Comparator.comparingInt(o -> o.getPath().length()));

        var visited = new HashSet<VirtualFile>();
        for (var iterator = roots.iterator(); iterator.hasNext(); ) {
            VirtualFile root = iterator.next();
            if (VfsUtil.isUnder(root, visited)) {
                iterator.remove();
            } else {
                visited.add(root);
            }
        }

        return roots.toArray(VirtualFile.EMPTY_ARRAY);
    }
}
