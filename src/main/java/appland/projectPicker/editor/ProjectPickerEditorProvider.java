package appland.projectPicker.editor;

import appland.projectPicker.LanguageAnalyzers;
import appland.projectPicker.LanguageResolver;
import appland.projectPicker.Languages;
import appland.projectPicker.Result;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.ui.jcef.JBCefApp;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ProjectPickerEditorProvider implements FileEditorProvider, DumbAware {
    private static final Key<Boolean> TYPE_KEY = Key.create("appland.projectPicker");
    static final Gson gson = new GsonBuilder().create();

    public static void open(@NotNull Project project) {
        var results = scanProjectAsync(project);
        if (results == null) {
            return;
        }

        var dummyFile = new LightVirtualFile("Project Picker", results);
        TYPE_KEY.set(dummyFile, true);
        FileEditorManager.getInstance(project).openFile(dummyFile, true);
    }

    /**
     * Scan the project's content roots under progress.
     *
     * @param project The project to scan
     * @return The JSON to load into the web application
     */
    private static @Nullable String scanProjectAsync(@NotNull Project project) {
        return ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
            return ReadAction.compute(() -> {
                var projects = scanProject(project);
                return gson.toJson(projects);
            });
        }, "Scanning project", true, project);
    }

    private static List<Result> scanProject(@NotNull Project project) {
        var projects = new ArrayList<Result>();
        var resolver = new LanguageResolver();

        for (var root : findTopLevelContentRoots(project)) {
            var language = resolver.getLanguage(root);
            if (language != null) {
                var languageInfo = Languages.getLanguage(language);
                if (languageInfo != null) {
                    var analyzer = LanguageAnalyzers.create(languageInfo);
                    if (analyzer != null) {
                        var analyze = analyzer.analyze(root);
                        projects.add(analyze);
                    }
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

    @NotNull
    public static Boolean isSupportedFile(@NotNull VirtualFile file) {
        return TYPE_KEY.isIn(file);
    }

    @Override
    public boolean accept(@NotNull Project project, @NotNull VirtualFile file) {
        return JBCefApp.isSupported() && isSupportedFile(file);
    }

    @Override
    public @NotNull FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile file) {
        return new ProjectPickerEditor(project, file);
    }

    @Override
    public @NotNull @NonNls String getEditorTypeId() {
        return "appland.projectPicker";
    }

    @Override
    public @NotNull FileEditorPolicy getPolicy() {
        return FileEditorPolicy.HIDE_DEFAULT_EDITOR;
    }
}
