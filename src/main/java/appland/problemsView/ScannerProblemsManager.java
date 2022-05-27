package appland.problemsView;

import appland.problemsView.model.FindingsFileData;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.analysis.problemsView.Problem;
import com.intellij.analysis.problemsView.ProblemsListener;
import com.intellij.analysis.problemsView.ProblemsProvider;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.SlowOperations;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.concurrent.GuardedBy;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public class ScannerProblemsManager implements ProblemsProvider {
    private static final Logger LOG = Logger.getInstance(ScannerProblemsManager.class);
    private static final Gson GSON = new GsonBuilder().create();

    private final Project project;
    private final ProblemsListener publisher;

    private final Object lock = new Object();
    // problem holder -> problem
    @GuardedBy("lock")
    private final Multimap<VirtualFile, ScannerProblem> problems = MultimapBuilder.hashKeys().arrayListValues().build();
    // findings file -> reference problem holders
    @GuardedBy("lock")
    private final Multimap<String, VirtualFile> sourceMapping = MultimapBuilder.hashKeys().hashSetValues().build();

    public static @NotNull ScannerProblemsManager getInstance(@NotNull Project project) {
        return project.getService(ScannerProblemsManager.class);
    }

    public ScannerProblemsManager(Project project) {
        this.project = project;
        this.publisher = project.getMessageBus().syncPublisher(ProblemsListener.TOPIC);
    }

    public static boolean isFindingFile(String path) {
        return path.endsWith("appmap-findings.json");
    }

    @NotNull
    @Override
    public Project getProject() {
        return project;
    }

    public Collection<VirtualFile> getProblemFiles() {
        synchronized (lock) {
            return List.copyOf(problems.keySet());
        }
    }

    public int getProblemFileCount() {
        synchronized (lock) {
            return problems.size();
        }
    }

    public List<Problem> getProblems(@NotNull VirtualFile file) {
        synchronized (lock) {
            return List.copyOf(problems.get(file));
        }
    }

    public int getProblemCount(@NotNull VirtualFile file) {
        synchronized (lock) {
            return problems.get(file).size();
        }
    }

    public void loadAllFindingFiles() {
        SlowOperations.allowSlowOperations(() -> {
            var manager = ScannerProblemsManager.getInstance(getProject());
            for (var findingFile : manager.findFindingsFiles()) {
                addFindingsFile(findingFile);
            }
        });
    }

    public void addFindingsFile(@NotNull VirtualFile findingsFile) {
        if (isNotUnderContentRoot(findingsFile)) {
            return;
        }

        synchronized (lock) {
            loadFileLocked(findingsFile, publisher::problemAppeared);
        }
    }

    public void reloadFindingsFile(@NotNull VirtualFile findingsFile) {
        if (isNotUnderContentRoot(findingsFile)) {
            return;
        }

        synchronized (lock) {
            for (var oldMapping : sourceMapping.removeAll(findingsFile.getPath())) {
                problems.removeAll(oldMapping);
            }

            loadFileLocked(findingsFile, publisher::problemUpdated);
        }
    }

    public void removeFindingsFile(@NotNull String path) {
        synchronized (lock) {
            for (var targetFile : sourceMapping.removeAll(path)) {
                for (var problem : problems.removeAll(targetFile)) {
                    publisher.problemDisappeared(problem);
                }
            }
        }
    }

    private void loadFileLocked(@NotNull VirtualFile findingsFile, @NotNull Consumer<Problem> notifier) {
        var fileData = loadFindingsFile(findingsFile);
        if (fileData != null) {
            for (var finding : fileData.findings) {
                var targetFile = finding.findTargetFile(getProject(), findingsFile);
                if (targetFile != null) {
                    sourceMapping.put(findingsFile.getPath(), targetFile);

                    var problem = new ScannerProblem(this, targetFile, finding);
                    problems.put(targetFile, problem);

                    notifier.accept(problem);
                }
            }
        }
    }

    private boolean isNotUnderContentRoot(@NotNull VirtualFile findingsFile) {
        var root = ProjectRootManager.getInstance(project).getFileIndex().getContentRootForFile(findingsFile, false);
        if (root == null) {
            LOG.warn("Findings file not managed by current project: " + findingsFile.getPath());
            return true;
        }
        return false;
    }

    public @NotNull Collection<VirtualFile> findFindingsFiles() {
        var scope = GlobalSearchScope.projectScope(project);
        return FilenameIndex.getVirtualFilesByName(project, "appmap-findings.json", true, scope);
    }

    private @Nullable FindingsFileData loadFindingsFile(@NotNull VirtualFile file) {
        var doc = FileDocumentManager.getInstance().getDocument(file);
        if (doc == null) {
            return null;
        }

        return GSON.fromJson(doc.getText(), FindingsFileData.class);
    }

    @Override
    public void dispose() {
    }
}
