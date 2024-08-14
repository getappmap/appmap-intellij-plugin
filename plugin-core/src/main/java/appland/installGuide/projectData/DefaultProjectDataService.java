package appland.installGuide.projectData;

import appland.AppMapBundle;
import appland.files.AppMapFiles;
import appland.index.AppMapFindingsUtil;
import appland.index.AppMapMetadata;
import appland.index.AppMapMetadataService;
import appland.problemsView.FindingsManager;
import appland.settings.AppMapProjectSettingsService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScopes;
import com.intellij.util.concurrency.annotations.RequiresReadLock;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
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

            return ReadAction.compute(() -> Arrays.stream(AppMapFiles.findTopLevelContentRoots(project))
                    .map(this::createMetadata)
                    .collect(Collectors.toList()));
        }, AppMapBundle.get("installGuide.scanningProject"), true, project);

        if (updatedProjects != null) {
            cachedProjects.set(updatedProjects);
        }
    }

    @RequiresReadLock
    private @NotNull ProjectMetadata createMetadata(@NotNull VirtualFile root) {
        // support fallback to non-NIO path for our tests
        var nioPath = root.getFileSystem().getNioPath(root);
        var path = nioPath != null ? nioPath.toString() : root.getPath();

        var allAppMaps = AppMapMetadataService.getInstance(project).findAppMaps();

        var findingsManager = FindingsManager.getInstance(project);
        var numFindings = findingsManager.getProblemCount();

        var projectSettings = AppMapProjectSettingsService.getState(project);
        var appMapConfigs = AppMapFiles.findAppMapConfigFiles(project);
        var investigatedFindings = projectSettings.isInvestigatedFindings();

        var module = ProjectRootManager.getInstance(project).getFileIndex().getModuleForFile(root);
        var isJavaProject = module != null && isJavaModule(module);

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

    private boolean isJavaModule(@NotNull Module module) {
        return "JAVA_MODULE".equals(ModuleType.get(module).getId());
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
}
