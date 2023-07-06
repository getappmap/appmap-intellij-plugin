package appland.installGuide.projectData;

import appland.AppMapBundle;
import appland.files.AppMapFiles;
import appland.index.*;
import appland.installGuide.analyzer.*;
import appland.problemsView.FindingsManager;
import appland.settings.AppMapProjectSettingsService;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.PathEnvironmentVariableUtil;
import com.intellij.execution.process.CapturingProcessRunner;
import com.intellij.execution.process.KillableProcessHandler;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.concurrency.annotations.RequiresReadLock;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class DefaultProjectDataService implements ProjectDataService {
    private static final Logger LOG = Logger.getInstance(DefaultProjectDataService.class);
    private static final IntSet supportedNodeMajorVersions = IntSet.of(14, 16, 18);
    private static final int NUMBER_OF_SAMPLE_CODE_OBJECTS = 5;

    private final Project project;
    private final AtomicReference<@NotNull List<@NotNull ProjectMetadata>> cachedProjects = new AtomicReference<>(List.of());

    public DefaultProjectDataService(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public @NotNull List<ProjectMetadata> getAppMapProjects() {
        updateMetadata();
        return cachedProjects.get();
    }

    private void updateMetadata() {
        var updatedProjects = ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
            var indicator = ProgressManager.getGlobalProgressIndicator();
            assert indicator != null;

            // we need a read action to locate the roots,
            // but we must not locate the node executables under a read action
            var roots = ReadAction.compute(this::scanContentRoots);

            var nodeVersions = new HashMap<VirtualFile, NodeVersion>();
            for (var root : roots.keySet()) {
                var nodeVersion = findNodeVersion(indicator, root);
                if (nodeVersion != null) {
                    nodeVersions.put(root, nodeVersion);
                }
            }

            return ReadAction.compute(() -> roots
                    .entrySet()
                    .stream()
                    .map(pair -> createMetadata(pair, nodeVersions.get(pair.getKey())))
                    .collect(Collectors.toList()));
        }, AppMapBundle.get("installGuide.scanningProject"), true, project);

        if (updatedProjects != null) {
            cachedProjects.set(updatedProjects);
        }
    }

    @RequiresReadLock
    private @NotNull ProjectMetadata createMetadata(@NotNull Map.Entry<VirtualFile, ProjectAnalysis> rootToAnalysis,
                                                    @Nullable NodeVersion nodeVersion) {
        var root = rootToAnalysis.getKey();
        var analysis = rootToAnalysis.getValue();

        // support fallback to non-NIO path for our tests
        var nioPath = root.getFileSystem().getNioPath(root);
        var path = nioPath != null ? nioPath.toString() : root.getPath();

        var allAppMaps = AppMapMetadataService.getInstance(project).findAppMaps();
        var bestAppMaps = allAppMaps
                .stream()
                .filter(AppMapMetadata::hasAnyCount)
                .sorted(Comparator.comparingInt(AppMapMetadata::getSortCount).reversed())
                .limit(10)
                .collect(Collectors.toList());

        var findingsManager = FindingsManager.getInstance(project);
        var numFindings = findingsManager.getProblemCount();
        var impactDomains = findingsManager.getFindingsImpactDomainCount();

        var sampleCodeObjects = findSampleCodeObjects(project);

        var projectSettings = AppMapProjectSettingsService.getState(project);
        var appMapConfigs = AppMapFiles.findAppMapConfigFiles(project, AppMapSearchScopes.projectFilesWithExcluded(project));
        // fixme
        var investigatedFindings = false;

        return ProjectMetadata.builder()
                .name(root.getPresentableName())
                .path(path)
                .agentInstalled(!appMapConfigs.isEmpty())
                .appMapOpened(projectSettings.isOpenedAppMapEditor())
                .generatedOpenApi(projectSettings.isCreatedOpenAPI())
                .investigatedFindings(investigatedFindings)
                .score(analysis.getScore())
                .analysisPerformed(isAnalysisPerformed(root))
                .hasNode(isNodeSupported(nodeVersion))
                .numFindings(numFindings)
                .findingsDomainCounts(impactDomains)
                .numHttpRequests(countRoutes(allAppMaps))
                .numAppMaps(allAppMaps.size())
                .appMaps(bestAppMaps)
                .sampleCodeObjects(sampleCodeObjects)
                .language(mapLanguageMetadata(analysis.getFeatures().getLang()))
                .testFramework(mapLanguageMetadata(analysis.getFeatures().getTest()))
                .webFramework(mapLanguageMetadata(analysis.getFeatures().getWeb()))
                .build();
    }

    private @Nullable SampleCodeObjects findSampleCodeObjects(@NotNull Project project) {
        var queriesToAppMaps = ClassMapTypeIndex.findItems(project, ClassMapItemType.Query);
        var requestsToAppMaps = ClassMapTypeIndex.findItems(project, ClassMapItemType.Route);
        if (queriesToAppMaps.isEmpty() && requestsToAppMaps.isEmpty()) {
            return null;
        }

        var requests = truncateSampleCodeObjects(getSampleCodeObjects(requestsToAppMaps));
        var queries = truncateSampleCodeObjects(getSampleCodeObjects(queriesToAppMaps));
        return new SampleCodeObjects(requests, queries);
    }

    private List<SimpleCodeObject> getSampleCodeObjects(Map<ClassMapItem, List<VirtualFile>> itemToAppMaps) {
        var singleFileEntries = itemToAppMaps.entrySet().stream()
                .filter(entry -> entry.getValue().size() == 1)
                .collect(Collectors.toList());

        if (singleFileEntries.size() >= NUMBER_OF_SAMPLE_CODE_OBJECTS) {
            return chooseClassMapItems(singleFileEntries);
        }

        return chooseClassMapItems(itemToAppMaps.entrySet().stream()
                .filter(entry -> !entry.getValue().isEmpty())
                .collect(Collectors.toList()));
    }

    private List<SimpleCodeObject> truncateSampleCodeObjects(List<SimpleCodeObject> codeObjects) {
        return codeObjects.stream().map(SimpleCodeObject::asTruncatedObject).collect(Collectors.toList());
    }

    /**
     * @return A selection of items from the list
     */
    private static @NotNull List<SimpleCodeObject> chooseClassMapItems(@NotNull Collection<Map.Entry<ClassMapItem, List<VirtualFile>>> items) {
        if (items.isEmpty()) {
            return Collections.emptyList();
        }

        var sorted = new ArrayList<>(items);
        sorted.sort(Comparator.comparing(entry -> entry.getKey().getId()));
        return sorted.subList(0, Math.min(NUMBER_OF_SAMPLE_CODE_OBJECTS, sorted.size())).stream()
                .map(item -> {
                    // sort to replicate VSCode's implementation
                    var path = item.getValue().stream().map(VirtualFile::getPath).sorted().findFirst();
                    return new SimpleCodeObject(item.getKey().getName(), path.orElse(null));
                })
                .collect(Collectors.toList());
    }

    private @Nullable ProjectMetadataFeature mapLanguageMetadata(@Nullable Feature feature) {
        if (feature == null) {
            return null;
        }

        return new ProjectMetadataFeature(feature.getTitle(), feature.getScore().getScoreValue() + 1, feature.getText());
    }

    private @NotNull Integer countRoutes(List<AppMapMetadata> appMaps) {
        return appMaps.stream().mapToInt(AppMapMetadata::getRequestCount).sum();
    }

    private boolean isAnalysisPerformed(@NotNull VirtualFile root) {
        return FindingsManager.getInstance(project).getProblemFileCount(root) > 0;
    }

    private boolean isNodeSupported(@Nullable NodeVersion nodeVersion) {
        return nodeVersion != null
                && nodeVersion.getMajor() != 0
                && supportedNodeMajorVersions.contains(nodeVersion.getMajor());
    }

    private Map<VirtualFile, ProjectAnalysis> scanContentRoots() {
        var projects = new HashMap<VirtualFile, ProjectAnalysis>();
        var resolver = new LanguageResolver(project);

        for (var root : findTopLevelContentRoots()) {
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

    private @NotNull VirtualFile[] findTopLevelContentRoots() {
        var roots = new ArrayList<>(List.of(ProjectRootManager.getInstance(project).getContentRoots()));
        roots.sort(Comparator.comparingInt(o -> o.getPath().length()));

        var visited = new HashSet<VirtualFile>();
        for (var iterator = roots.iterator(); iterator.hasNext(); ) {
            var root = iterator.next();
            if (VfsUtil.isUnder(root, visited)) {
                iterator.remove();
            } else {
                visited.add(root);
            }
        }

        return roots.toArray(VirtualFile.EMPTY_ARRAY);
    }

    private @Nullable NodeVersion findNodeVersion(@NotNull ProgressIndicator indicator, @NotNull VirtualFile rootFolder) {
        // nvm version
        var nvmFile = rootFolder.findChild(".nvmrc");
        if (nvmFile != null) {
            var nodeVersion = parseNvmFile(nvmFile);
            if (nodeVersion != null) {
                return nodeVersion;
            }
        }

        // system node version
        var nodeExecutable = PathEnvironmentVariableUtil.findExecutableInPathOnAnyOS("node");
        if (nodeExecutable != null) {
            return parseStdoutNodeVersion(indicator, nodeExecutable);
        }

        // null as default
        return null;
    }

    @Nullable
    private static NodeVersion parseNvmFile(VirtualFile nvmFile) {
        try {
            var text = VfsUtilCore.loadText(nvmFile);
            var nodeVersion = NodeVersion.parse(text);
            if (nodeVersion != null) {
                return nodeVersion;
            }
        } catch (IOException e) {
            LOG.debug("Error parsing .nvmrc file, path: " + nvmFile.getPath(), e);
        }
        return null;
    }

    @Nullable
    private static NodeVersion parseStdoutNodeVersion(@NotNull ProgressIndicator indicator, @NotNull File node) {
        try {
            var cmdline = new GeneralCommandLine(node.getPath(), "-v");
            var process = new CapturingProcessRunner(new KillableProcessHandler(cmdline));
            var output = process.runProcess(indicator, (int) TimeUnit.SECONDS.toMillis(1), true);
            if (output.getExitCode() == 0) {
                var nodeVersion = NodeVersion.parse(output.getStdout());
                if (nodeVersion != null) {
                    return nodeVersion;
                }
            }
        } catch (Exception e) {
            LOG.debug("Failed to launch node -v, node: " + node, e);
        }
        return null;
    }
}
