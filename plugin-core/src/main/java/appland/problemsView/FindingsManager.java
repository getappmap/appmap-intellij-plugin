package appland.problemsView;

import appland.index.AppMapFindingsUtil;
import appland.index.AppMapSearchScopes;
import appland.problemsView.listener.ScannerFindingsListener;
import appland.problemsView.model.FindingsDomainCount;
import appland.problemsView.model.FindingsFileData;
import appland.problemsView.model.ScannerFinding;
import appland.utils.GsonUtils;
import com.google.common.collect.*;
import com.google.gson.JsonSyntaxException;
import com.intellij.analysis.problemsView.Problem;
import com.intellij.analysis.problemsView.ProblemsProvider;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.concurrency.annotations.RequiresReadLock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.concurrent.GuardedBy;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
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
    // findings file path -> annotated source files
    @GuardedBy("lock")
    private final Multimap<String, VirtualFile> findingPathToSourceFiles = MultimapBuilder.hashKeys().hashSetValues().build();
    // findings files path -> findings without source files
    @GuardedBy("lock")
    private final Multimap<String, ScannerFinding> findingPathToFindings = MultimapBuilder.hashKeys().hashSetValues().build();
    // reverse mapping: annotated source file -> problem representing finding and source file
    @GuardedBy("lock")
    private final Multimap<VirtualFile, ScannerProblem> sourceFileToProblems = MultimapBuilder.hashKeys().arrayListValues().build();
    // problems where the target file couldn't be found
    @GuardedBy("lock")
    private final Multiset<ScannerFinding> findingsWithoutSourceFiles = HashMultiset.create();

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

    /**
     * @return All known findings, including findings without known source files in the project.
     */
    public @NotNull List<ScannerFinding> getAllFindings() {
        synchronized (lock) {
            return Streams.concat(
                    sourceFileToProblems.values().stream().map(ScannerProblem::getFinding),
                    findingsWithoutSourceFiles.stream()
            ).collect(Collectors.toList());
        }
    }

    public int getProblemFileCount() {
        synchronized (lock) {
            return sourceFileToProblems.size();
        }
    }

    public @NotNull List<ScannerProblem> getAllProblems() {
        synchronized (lock) {
            return List.copyOf(sourceFileToProblems.values());
        }
    }

    /**
     * @return List of files, which are associated with problems.
     */
    public @NotNull Collection<VirtualFile> getProblemFiles() {
        synchronized (lock) {
            return List.copyOf(sourceFileToProblems.keySet());
        }
    }

    public List<Problem> getProblems(@NotNull VirtualFile file) {
        synchronized (lock) {
            return List.copyOf(sourceFileToProblems.get(file));
        }
    }

    public List<ScannerProblem> getScannerProblems(@NotNull VirtualFile file) {
        synchronized (lock) {
            return List.copyOf(sourceFileToProblems.get(file));
        }
    }

    /**
     * @return The total number of findings for this project.
     */
    public int getProblemCount() {
        synchronized (lock) {
            return sourceFileToProblems.size() + findingsWithoutSourceFiles.size();
        }
    }

    /**
     * @param file The file associated with problems
     * @return The number of problems found in the file
     */
    public int getProblemCount(@NotNull VirtualFile file) {
        synchronized (lock) {
            return sourceFileToProblems.get(file).size();
        }
    }

    /**
     * @return Counts of impact domain of all findings of this project.
     */
    public @NotNull FindingsDomainCount getFindingsImpactDomainCount() {
        var domainMapping = new FindingsDomainCount();
        synchronized (lock) {
            for (var problem : sourceFileToProblems.values()) {
                var finding = problem.getFinding();
                if (finding.impactDomain != null) {
                    domainMapping.add(finding.impactDomain);
                }
            }

            for (var finding : findingPathToFindings.values()) {
                if (finding.impactDomain != null) {
                    domainMapping.add(finding.impactDomain);
                }
            }
        }
        return domainMapping;
    }

    public int getOtherProblemCount() {
        synchronized (lock) {
            return findingsWithoutSourceFiles.size();
        }
    }

    public Collection<ScannerFinding> getOtherProblems() {
        synchronized (lock) {
            return List.copyOf(findingsWithoutSourceFiles);
        }
    }

    public void reloadAsync() {
        // the non-blocking ReadAction must not have side effects, because it may be executed multiple times
        ReadAction.nonBlocking(this::findFindingsFiles)
                .inSmartMode(project)
                .expireWith(this)
                .coalesceBy(getClass(), project)
                .finishOnUiThread(ModalityState.any(), findingFiles -> {
                    if (project.isDisposed()) {
                        return;
                    }

                    // We can't use "submit().onSuccess()" to process the files, because the non-blocking ReadAction
                    // cancels the progress indicator, which is wrapping the code of "onSuccess()". But loading the
                    // findings must not be cancelled, so we're moving into our own ReadAction outside the original
                    // progress indicator.
                    ApplicationManager.getApplication().executeOnPooledThread(() -> {
                        if (project.isDisposed()) {
                            return;
                        }

                        ReadAction.run(() -> {
                            if (project.isDisposed()) {
                                return;
                            }

                            clearAndNotify();
                            for (var findingFile : findingFiles) {
                                addFindingsFile(findingFile);
                            }
                            project.getMessageBus().syncPublisher(ScannerFindingsListener.TOPIC).afterFindingsReloaded();
                        });
                    });
                }).submit(AppExecutorUtil.getAppExecutorService());
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

    public @NotNull List<ScannerFinding> findFindingsByHash(@NotNull String hashV2) {
        synchronized (lock) {
            return sourceFileToProblems.values().stream()
                    .map(ScannerProblem::getFinding)
                    .filter(p -> hashV2.equals(p.getAppMapHashWithFallback()))
                    .collect(Collectors.toList());
        }
    }

    @Override
    public void dispose() {
        reset();
    }

    void reset() {
        synchronized (lock) {
            findingPathToSourceFiles.clear();
            sourceFileToProblems.clear();
            findingPathToFindings.clear();
            findingsWithoutSourceFiles.clear();
        }
    }

    private void clearAndNotify() {
        synchronized (lock) {
            var allPaths = new HashSet<String>();
            allPaths.addAll(findingPathToSourceFiles.keySet());
            allPaths.addAll(findingPathToFindings.keySet());

            for (var path : allPaths) {
                removeFindingsFileLocked(path);
            }

            assert findingPathToSourceFiles.isEmpty();
            assert findingPathToFindings.isEmpty();
            assert sourceFileToProblems.isEmpty();
            assert findingsWithoutSourceFiles.isEmpty();
        }
    }

    private void removeFindingsFileLocked(@NotNull String path) {
        var dataChanged = false;

        for (var annotatedSourceFile : findingPathToSourceFiles.removeAll(path)) {
            for (var problem : sourceFileToProblems.removeAll(annotatedSourceFile)) {
                publisher.problemDisappeared(problem);
                dataChanged = true;
            }
        }

        var unknownFileMapping = findingPathToFindings.removeAll(path);
        if (!unknownFileMapping.isEmpty()) {
            findingsWithoutSourceFiles.removeAll(unknownFileMapping);
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
                finding.setFindingsFile(findingsFile);

                // multiple metadata values exist to support scanner batch mode,
                // but this is mostly deprecated now and shouldn't exist for user-generated AppMap data.
                if (fileData.metadata != null && !fileData.metadata.isEmpty()) {
                    var metaData = fileData.metadata.values().iterator().next();
                    finding.setFindingsMetaData(metaData);
                }

                var annotatedSourceFile = finding.findAnnotatedSourceFile(project, findingsFile);
                if (annotatedSourceFile != null) {
                    findingPathToSourceFiles.put(findingsFile.getPath(), annotatedSourceFile);

                    var problem = new ScannerProblem(this, annotatedSourceFile, finding);
                    sourceFileToProblems.put(annotatedSourceFile, problem);

                    // this takes care of problemAppeared, problemDisappeared, problemUpdated notifications
                    notifier.accept(problem);
                } else {
                    findingPathToFindings.put(findingsFile.getPath(), finding);
                    findingsWithoutSourceFiles.add(finding);

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
        var scope = AppMapSearchScopes.projectFilesWithExcluded(project);
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
