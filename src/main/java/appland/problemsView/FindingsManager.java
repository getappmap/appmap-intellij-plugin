package appland.problemsView;

import appland.problemsView.model.FindingsFileData;
import appland.problemsView.model.ScannerFinding;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Multiset;
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

public class FindingsManager implements ProblemsProvider {
    private static final Logger LOG = Logger.getInstance(FindingsManager.class);
    private static final Gson GSON = new GsonBuilder().create();

    private final Project project;
    private final ProblemsListener publisher;
    private final UnknownFileProblemListener unknownFilePublisher;

    private final Object lock = new Object();
    // findings file -> reference problem holders
    @GuardedBy("lock")
    private final Multimap<String, VirtualFile> sourceMapping = MultimapBuilder.hashKeys().hashSetValues().build();
    // problem holder -> problem
    @GuardedBy("lock")
    private final Multimap<VirtualFile, ScannerProblem> problems = MultimapBuilder.hashKeys().arrayListValues().build();
    // findings files with unknown problem target files
    @GuardedBy("lock")
    private final Multimap<String, ScannerFinding> sourceMappingOther = MultimapBuilder.hashKeys().hashSetValues().build();
    // problems where the target file couldn't be found
    @GuardedBy("lock")
    private final Multiset<ScannerFinding> problemsOther = HashMultiset.create();

    public static @NotNull FindingsManager getInstance(@NotNull Project project) {
        return project.getService(FindingsManager.class);
    }

    public FindingsManager(Project project) {
        this.project = project;

        var messageBus = project.getMessageBus();
        this.publisher = messageBus.syncPublisher(ProblemsListener.TOPIC);
        this.unknownFilePublisher = messageBus.syncPublisher(UnknownFileProblemListener.TOPIC);
    }

    public static boolean isFindingFile(String path) {
        return path.endsWith("appmap-findings.json");
    }

    @NotNull
    @Override
    public Project getProject() {
        return project;
    }

    public int getProblemFileCount() {
        synchronized (lock) {
            return problems.size();
        }
    }

    public Collection<VirtualFile> getProblemFiles() {
        synchronized (lock) {
            return List.copyOf(problems.keySet());
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

    public int getOtherProblemCount() {
        synchronized (lock) {
            return problemsOther.size();
        }
    }

    public Collection<ScannerFinding> getOtherProblems() {
        synchronized (lock) {
            return List.copyOf(problemsOther);
        }
    }

    public void loadAllFindingFiles() {
        SlowOperations.allowSlowOperations(() -> {
            var manager = FindingsManager.getInstance(getProject());
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

            var unknownFileMapping = sourceMappingOther.removeAll(path);
            if (!unknownFileMapping.isEmpty()) {
                problemsOther.removeAll(unknownFileMapping);
                unknownFilePublisher.unknownFileProblemsChanged();
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
                } else {
                    sourceMappingOther.put(findingsFile.getPath(), finding);
                    problemsOther.add(finding);

                    unknownFilePublisher.unknownFileProblemsChanged();
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
