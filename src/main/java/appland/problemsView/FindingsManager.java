package appland.problemsView;

import appland.index.AppMapFindingsUtil;
import appland.problemsView.model.FindingsDomainCount;
import appland.problemsView.model.FindingsFileData;
import appland.problemsView.model.ScannerFinding;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Multiset;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.intellij.analysis.problemsView.Problem;
import com.intellij.analysis.problemsView.ProblemsListener;
import com.intellij.analysis.problemsView.ProblemsProvider;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.concurrency.annotations.RequiresReadLock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.concurrency.CancellablePromise;

import javax.annotation.concurrent.GuardedBy;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;

/**
 * Service managing the AppMap findings of a project.
 */
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

    public FindingsManager(@NotNull Project project) {
        this.project = project;

        var messageBus = project.getMessageBus();
        this.publisher = messageBus.syncPublisher(ProblemsListener.TOPIC);
        this.unknownFilePublisher = messageBus.syncPublisher(UnknownFileProblemListener.TOPIC);
    }

    @Override
    public @NotNull Project getProject() {
        return project;
    }

    public int getProblemFileCount() {
        synchronized (lock) {
            return problems.size();
        }
    }

    /**
     * @param root Root directory to limit the scope
     * @return The number of problem files, which are located under root
     */
    public int getProblemFileCount(@NotNull VirtualFile root) {
        synchronized (lock) {
            return (int) problems.keySet()
                    .stream()
                    .filter(file -> VfsUtilCore.isAncestor(root, file, false))
                    .count();
        }
    }

    /**
     * @return List of files, which are associated with problems.
     */
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

    public List<ScannerProblem> getScannerProblems(@NotNull VirtualFile file) {
        synchronized (lock) {
            return List.copyOf(problems.get(file));
        }
    }

    /**
     * @return The total number of findings for this project.
     */
    public int getProblemCount() {
        synchronized (lock) {
            return problems.size() + problemsOther.size();
        }
    }

    /**
     * @param file The file associated with problems
     * @return Tne number of problems found in the file
     */
    public int getProblemCount(@NotNull VirtualFile file) {
        synchronized (lock) {
            return problems.get(file).size();
        }
    }

    /**
     * @return Counts of impact domain of all findings of this project.
     */
    public @NotNull FindingsDomainCount getFindingsImpactDomainCount() {
        var domainMapping = new FindingsDomainCount();
        synchronized (lock) {
            for (var problem : problems.values()) {
                var finding = problem.getFinding();
                if (finding.impactDomain != null) {
                    domainMapping.add(finding.impactDomain);
                }
            }

            for (var finding : sourceMappingOther.values()) {
                if (finding.impactDomain != null) {
                    domainMapping.add(finding.impactDomain);
                }
            }
        }
        return domainMapping;
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

    public @NotNull CancellablePromise<Void> reloadAsync() {
        return ReadAction.nonBlocking(this::doReload)
                .inSmartMode(project)
                .expireWith(this)
                .coalesceBy(this)
                .submit(AppExecutorUtil.getAppExecutorService());
    }

    public void addFindingsFile(@NotNull VirtualFile findingsFile) {
        if (isNotUnderContentRoot(findingsFile)) {
            return;
        }

        synchronized (lock) {
            // avoid duplicates for registered files
            removeFindingsFileLocked(findingsFile.getPath());
            loadFileLocked(findingsFile, publisher::problemAppeared);
        }
    }

    public void reloadFindingsFile(@NotNull VirtualFile findingsFile) {
        if (isNotUnderContentRoot(findingsFile)) {
            return;
        }

        synchronized (lock) {
            removeFindingsFileLocked(findingsFile.getPath());
            loadFileLocked(findingsFile, publisher::problemUpdated);
        }
    }

    public void removeFindingsFile(@NotNull String path) {
        synchronized (lock) {
            removeFindingsFileLocked(path);
        }
    }

    @Override
    public void dispose() {
        reset();
    }

    void reset() {
        synchronized (lock) {
            sourceMapping.clear();
            problems.clear();
            sourceMappingOther.clear();
            problemsOther.clear();
        }
    }

    private void doReload() {
        clearAndNotify();

        for (var findingFile : findFindingsFiles()) {
            addFindingsFile(findingFile);
        }

        project.getMessageBus().syncPublisher(FindingsReloadedListener.TOPIC).afterFindingsReloaded();
    }

    private void clearAndNotify() {
        synchronized (lock) {
            var allPaths = new HashSet<String>();
            allPaths.addAll(sourceMapping.keySet());
            allPaths.addAll(sourceMappingOther.keySet());

            for (var path : allPaths) {
                removeFindingsFileLocked(path);
            }

            assert sourceMapping.isEmpty();
            assert sourceMappingOther.isEmpty();
            assert problems.isEmpty();
            assert problemsOther.isEmpty();
        }
    }

    private void removeFindingsFileLocked(@NotNull String path) {
        for (var targetFile : sourceMapping.removeAll(path)) {
            for (var problem : problems.removeAll(targetFile)) {
                publisher.problemDisappeared(problem);
            }
        }

        var unknownFileMapping = sourceMappingOther.removeAll(path);
        if (!unknownFileMapping.isEmpty()) {
            problemsOther.removeAll(unknownFileMapping);
            unknownFilePublisher.afterUnknownFileProblemsChange();
        }
    }

    private void loadFileLocked(@NotNull VirtualFile findingsFile, @NotNull Consumer<Problem> notifier) {
        var fileData = loadFindingsFile(findingsFile);
        if (fileData != null) {
            for (var finding : fileData.findings) {
                var targetFile = finding.findTargetFile(project, findingsFile);
                if (targetFile != null) {
                    sourceMapping.put(findingsFile.getPath(), targetFile);

                    var problem = new ScannerProblem(this, targetFile, finding);
                    problems.put(targetFile, problem);

                    notifier.accept(problem);
                } else {
                    sourceMappingOther.put(findingsFile.getPath(), finding);
                    problemsOther.add(finding);

                    unknownFilePublisher.afterUnknownFileProblemsChange();
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

    @RequiresReadLock
    private @NotNull Collection<VirtualFile> findFindingsFiles() {
        // only "everything" seems to contain our appmap-findings.json files in excluded parent folders
        var scope = GlobalSearchScope.everythingScope(project);
        return FilenameIndex.getVirtualFilesByName(project, AppMapFindingsUtil.FINDINGS_FILE_NAME, true, scope);
    }

    private @Nullable FindingsFileData loadFindingsFile(@NotNull VirtualFile file) {
        var doc = FileDocumentManager.getInstance().getDocument(file);
        if (doc == null || doc.getTextLength() == 0) {
            return null;
        }

        try {
            return GSON.fromJson(doc.getText(), FindingsFileData.class);
        } catch (JsonSyntaxException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Failed to load findings file: " + file.getPath(), e);
            }
            return null;
        }
    }
}
