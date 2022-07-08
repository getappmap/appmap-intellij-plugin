package appland.installGuide.projectData;

import appland.installGuide.languageAnalyzer.*;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class DefaultProjectDataService implements ProjectDataService {
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
            return ReadAction.compute(() -> {
                return scanProject()
                        .entrySet()
                        .stream()
                        .map(this::createMetadata)
                        .collect(Collectors.toList());
            });
        }, "Scanning project", true, project);

        if (updatedProjects != null) {
            cachedProjects.set(updatedProjects);
        }
    }

    private @NotNull ProjectMetadata createMetadata(@NotNull Map.Entry<VirtualFile, ProjectAnalysis> pair) {
        var root = pair.getKey();
        var analysis = pair.getValue();

        return ProjectMetadata.builder()
                .name(root.getPresentableName())
                .path(root.toNioPath().toString())
                .score(analysis.getScore())
                .hasNode(isNodeSupported(analysis.getNodeVersion()))
                .agentInstalled(isAgentInstalled(root))
                .appMapsRecorded(hasRecordedAppMaps(root))
                .analysisPerformed(isAnalysisPerformed())
                .appMapOpened(isAppMapOpened(root))
                .numFindings(getNumFindings())
                .language(mapLanguageMetadata(analysis.getFeatures().getLang()))
                .testFramework(mapLanguageMetadata(analysis.getFeatures().getTest()))
                .webFramework(mapLanguageMetadata(analysis.getFeatures().getWeb()))
                //.appmaps(analysis.appmaps)
                .build();
    }

    private @Nullable ProjectMetadataFeature mapLanguageMetadata(@Nullable Feature feature) {
        if (feature == null) {
            return null;
        }

        return new ProjectMetadataFeature(feature.getTitle(), feature.getScore().getScoreValue() + 1, feature.getText());
    }

    private @Nullable Integer getNumFindings() {
        return null;
    }

    private boolean isAnalysisPerformed() {
        return false;
    }

    private boolean isAppMapOpened(@NotNull VirtualFile root) {
        return false;
    }

    private boolean hasRecordedAppMaps(@NotNull VirtualFile root) {
        return false;
    }

    private boolean isAgentInstalled(@NotNull VirtualFile root) {
        return false;
    }

    private boolean isNodeSupported(@Nullable NodeVersion nodeVersion) {
        return nodeVersion != null
                && nodeVersion.getMajor() != 0
                && supportedNodeMajorVersions.contains(nodeVersion.getMajor());
    }

    private Map<VirtualFile, ProjectAnalysis> scanProject() {
        var projects = new HashMap<VirtualFile, ProjectAnalysis>();
        var resolver = new LanguageResolver();

        for (var root : findTopLevelContentRoots()) {
            var language = resolver.getLanguage(root);
            if (language != null) {
                var analyzer = LanguageAnalyzers.create(language);
                if (analyzer != null) {
                    projects.put(root, analyzer.analyze(root));
                }
            }
        }
        return projects;
    }

    @NotNull
    private VirtualFile[] findTopLevelContentRoots() {
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
}
