package appland.installGuide.projectData;

import appland.AppMapBundle;
import appland.files.AppMapFiles;
import appland.index.AppMapFindingsUtil;
import appland.index.AppMapMetadata;
import appland.index.AppMapMetadataService;
import appland.installGuide.analyzer.LanguageAnalyzer;
import appland.installGuide.analyzer.LanguageResolver;
import appland.installGuide.analyzer.ProjectAnalysis;
import appland.installGuide.analyzer.languages.JavaLanguageAnalyzer;
import appland.problemsView.FindingsManager;
import appland.settings.AppMapProjectSettingsService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScopes;
import com.intellij.util.concurrency.annotations.RequiresReadLock;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class DefaultProjectDataService implements ProjectDataService {
    private final Project project;
    private final AtomicReference<@NotNull List<@NotNull ProjectMetadata>> cachedProjects = new AtomicReference<>(List.of());

    public DefaultProjectDataService(@NotNull Project project) {
        this.project = project;
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public @NotNull List<ProjectMetadata> getAppMapProjects(boolean updateMetadata) {
        if (updateMetadata) {
            ApplicationManager.getApplication().assertReadAccessNotAllowed();
            updateMetadata();
        }
        return cachedProjects.get();
    }

    private void updateMetadata() {
        var updatedProjects = ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
            var indicator = ProgressManager.getGlobalProgressIndicator();
            assert indicator != null;

            // we need a read action to locate the roots,
            // but we must not locate the node executables under a read action
            var roots = ReadAction.compute(this::scanContentRoots);

            return ReadAction.compute(() -> roots
                    .entrySet()
                    .stream()
                    .map(this::createMetadata)
                    .collect(Collectors.toList()));
        }, AppMapBundle.get("installGuide.scanningProject"), true, project);

        if (updatedProjects != null) {
            cachedProjects.set(updatedProjects);
        }
    }

    @RequiresReadLock
    private @NotNull ProjectMetadata createMetadata(@NotNull Map.Entry<VirtualFile, ProjectAnalysis> rootToAnalysis) {
        var root = rootToAnalysis.getKey();
        var analysis = rootToAnalysis.getValue();

        // support fallback to non-NIO path for our tests
        var nioPath = root.getFileSystem().getNioPath(root);
        var path = nioPath != null ? nioPath.toString() : root.getPath();

        var allAppMaps = AppMapMetadataService.getInstance(project).findAppMaps();

        var findingsManager = FindingsManager.getInstance(project);
        var numFindings = findingsManager.getProblemCount();

        var projectSettings = AppMapProjectSettingsService.getState(project);
        var appMapConfigs = AppMapFiles.findAppMapConfigFiles(project);
        var investigatedFindings = projectSettings.isInvestigatedFindings();

        var isJavaProject = JavaLanguageAnalyzer.JAVA_LANGUAGE_TITLE.equals(analysis.getFeatures().getLang().title);

        return ProjectMetadata.builder()
                .name(root.getPresentableName())
                .path(path)
                .agentInstalled(!appMapConfigs.isEmpty() || isJavaProject)
                .appMapOpened(projectSettings.isOpenedAppMapEditor())
                .generatedOpenApi(projectSettings.isCreatedOpenAPI())
                .investigatedFindings(investigatedFindings)
                .analysisPerformed(isAnalysisPerformed(root))
                .numFindings(numFindings)
                .numHttpRequests(countRoutes(allAppMaps))
                .numAppMaps(allAppMaps.size())
                .build();
    }

    private @NotNull Integer countRoutes(List<AppMapMetadata> appMaps) {
        return appMaps.stream().mapToInt(AppMapMetadata::getRequestCount).sum();
    }

    /**
     * It's considered as "performed" if there's at least one appmap-findings.json under root,
     * event if it does not contain any findings.
     */
    @RequiresReadLock
    private boolean isAnalysisPerformed(@NotNull VirtualFile root) {
        var searchScope = GlobalSearchScopes.directoryScope(project, root, true);
        return AppMapFindingsUtil.isAnalysisPerformed(searchScope);
    }

    private Map<VirtualFile, ProjectAnalysis> scanContentRoots() {
        var projects = new HashMap<VirtualFile, ProjectAnalysis>();
        var resolver = new LanguageResolver(project);

        for (var root : AppMapFiles.findTopLevelContentRoots(project)) {
            var language = resolver.getLanguage(root);
            if (language != null) {
                var analyzer = LanguageAnalyzer.create(language);
                if (analyzer != null) {
                    projects.put(root, analyzer.analyze(root));
                }
            }
        }

        return projects;
    }
}
