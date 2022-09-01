package appland.installGuide.projectData;

import appland.AppMapBundle;
import appland.index.AppMapMetadata;
import appland.index.AppMapMetadataIndex;
import appland.installGuide.analyzer.*;
import appland.problemsView.FindingsManager;
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

        var allAppMaps = AppMapMetadataIndex.findAppMaps(project, null);
        var bestAppMaps = allAppMaps
                .stream()
                .filter(AppMapMetadata::hasAnyCount)
                .sorted(Comparator.comparingInt(AppMapMetadata::getSortCount).reversed())
                .limit(10)
                .collect(Collectors.toList());

        var findingsManager = FindingsManager.getInstance(project);
        var numFindings = findingsManager.getProblemCount();
        var impactDomains = findingsManager.getFindingsImpactDomainCount();

        // fixme remove this sample code
        var sampleCodeObjects = new SampleCodeObjects(
                List.of(new SimpleCodeObject("Request 1", "/path")),
                List.of(new SimpleCodeObject("Query 1", "/query")));

        return ProjectMetadata.builder()
                .name(root.getPresentableName())
                .path(path)
                .score(analysis.getScore())
                .hasNode(isNodeSupported(nodeVersion))
                .agentInstalled(isAgentInstalled(root))
                .appMapsRecorded(hasRecordedAppMaps(root))
                .appMapOpened(isAppMapOpened(root))
                .analysisPerformed(findingsManager.getProblemFileCount() > 0)
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

    private @Nullable ProjectMetadataFeature mapLanguageMetadata(@Nullable Feature feature) {
        if (feature == null) {
            return null;
        }

        return new ProjectMetadataFeature(feature.getTitle(), feature.getScore().getScoreValue() + 1, feature.getText());
    }

    private @NotNull Integer countRoutes(List<AppMapMetadata> appMaps) {
        return appMaps.stream().mapToInt(AppMapMetadata::getRequestCount).sum();
    }

    private boolean isAppMapOpened(@NotNull VirtualFile root) {
        return false;
    }

    private boolean hasRecordedAppMaps(@NotNull VirtualFile root) {
        return false;
    }

    private boolean isAgentInstalled(@NotNull VirtualFile root) {
        return root.findChild("appmap.yml") != null;
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
