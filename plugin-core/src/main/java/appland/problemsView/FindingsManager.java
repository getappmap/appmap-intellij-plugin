package appland.problemsView;

import appland.index.AppMapFindingsUtil;
import appland.problemsView.listener.ScannerFindingsListener;
import appland.problemsView.model.FindingsDomainCount;
import appland.problemsView.model.FindingsFileData;
import appland.problemsView.model.ImpactDomain;
import appland.problemsView.model.ScannerFinding;
import appland.utils.GsonUtils;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Multiset;
import com.google.gson.JsonSyntaxException;
import com.intellij.analysis.problemsView.Problem;
import com.intellij.analysis.problemsView.ProblemsProvider;
import com.intellij.openapi.application.ApplicationManager;
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
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Service managing the AppMap findings of a project.
 */
public class FindingsManager implements ProblemsProvider {
    private static final Logger LOG = Logger.getInstance(FindingsManager.class);

    private final Project project;
    private final ScannerFindingsListener publisher;

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
        this.publisher = project.getMessageBus().syncPublisher(ScannerFindingsListener.TOPIC);
    }

    @Override
    public @NotNull Project getProject() {
        return project;
    }

    public @NotNull List<ScannerFinding> getAllFindings() {
        synchronized (lock) {
            return problems.values().stream()
                    .map(ScannerProblem::getFinding)
                    .collect(Collectors.toList());
        }
    }

    public @NotNull List<ScannerProblem> getAllProblems() {
        synchronized (lock) {
            return List.copyOf(problems.values());
        }
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
     * @return The number of problems found in the file
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

    /**
     * @return Counts of impact domain of all findings of this project.
     */
    public @NotNull Map<ImpactDomain, List<ScannerProblemWithFile>> getProblemsByImpactDomain() {
        var groups = new EnumMap<ImpactDomain, List<ScannerProblemWithFile>>(ImpactDomain.class);

        synchronized (lock) {
            for (var entry : problems.entries()) {
                var finding = entry.getValue();
                var domain = finding.getFinding().impactDomain;
                if (domain != null) {
                    var sourceFile = entry.getKey();
                    if (groups.containsKey(domain)) {
                        groups.get(domain).add(new ScannerProblemWithFile(finding, sourceFile));
                    } else {
                        groups.put(domain, new ArrayList<>(List.of(new ScannerProblemWithFile(finding, sourceFile))));
                    }
                }
            }
        }

        return groups;
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

    public @NotNull List<ScannerProblem> findProblemByHash(@NotNull String hashV2) {
        synchronized (lock) {
            return problems.values().stream()
                    .filter(p -> hashV2.equals(p.getFinding().getAppMapHashWithFallback()))
                    .collect(Collectors.toList());
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

        project.getMessageBus().syncPublisher(ScannerFindingsListener.TOPIC).afterFindingsReloaded();
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
        var dataChanged = false;

        for (var targetFile : sourceMapping.removeAll(path)) {
            for (var problem : problems.removeAll(targetFile)) {
                publisher.problemDisappeared(problem);
                dataChanged = true;
            }
        }

        var unknownFileMapping = sourceMappingOther.removeAll(path);
        if (!unknownFileMapping.isEmpty()) {
            problemsOther.removeAll(unknownFileMapping);
            publisher.afterUnknownFileProblemsChange();
            dataChanged = true;
        }

        if (dataChanged) {
            publisher.afterFindingsChanged();
        }
    }

    private void loadFileLocked(@NotNull VirtualFile findingsFile, @NotNull Consumer<Problem> notifier) {
        var fileData = loadFindingsFile(findingsFile);
        if (fileData != null && fileData.findings != null) {
            for (var finding : fileData.findings) {
                var annotatedFile = finding.findAnnotatedFile(project, findingsFile);
                if (annotatedFile != null) {
                    sourceMapping.put(findingsFile.getPath(), annotatedFile);

                    var problem = new ScannerProblem(this, annotatedFile, finding, findingsFile);
                    problems.put(annotatedFile, problem);

                    // this takes care of problemAppeared, problemDisappeared, problemUpdated notifications
                    notifier.accept(problem);
                } else {
                    sourceMappingOther.put(findingsFile.getPath(), finding);
                    problemsOther.add(finding);

                    publisher.afterUnknownFileProblemsChange();
                }
            }

            publisher.afterFindingsChanged();
        }
    }

    private boolean isNotUnderContentRoot(@NotNull VirtualFile findingsFile) {
        if (ApplicationManager.getApplication().isUnitTestMode()) {
            return false;
        }

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
            var data = GsonUtils.GSON.fromJson(doc.getText(), FindingsFileData.class);
            if (data != null && data.findings != null && !data.findings.isEmpty()) {
                var ruleMapping = data.createRuleInfoMapping();
                for (var finding : data.findings) {
                    // update rule info with the ruleId of the finding
                    finding.ruleInfo = ruleMapping.get(finding.ruleId);
                }
            }
            return data;
        } catch (JsonSyntaxException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Failed to load findings file: " + file.getPath(), e);
            }
            return null;
        }
    }
}
