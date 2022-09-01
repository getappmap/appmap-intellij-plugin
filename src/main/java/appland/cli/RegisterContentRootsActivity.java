package appland.cli;

import appland.AppLandLifecycleService;
import com.intellij.ProjectTopics;
import com.intellij.execution.ExecutionException;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootEvent;
import com.intellij.openapi.roots.ModuleRootListener;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;

/**
 * Launch processes for the opened project and registers a listener to update the processes when content roots change.
 * Executed on the EDT when indexes are ready.
 */
@SuppressWarnings("UnstableApiUsage")
public class RegisterContentRootsActivity implements StartupActivity {
    private static final Logger LOG = Logger.getInstance(RegisterContentRootsActivity.class);

    @Override
    public void runActivity(@NotNull Project project) {
        // skip in our tests, e.g. to avoid interfering with the in-memory project roots
        if (ApplicationManager.getApplication().isUnitTestMode()) {
            return;
        }

        launchProcesses(project);

        listenForContentRootChanges(project, AppLandLifecycleService.getInstance(project));
    }

    private void launchProcesses(@NotNull Project project) {
        var downloadService = AppLandDownloadService.getInstance();
        if (!downloadService.isDownloaded(CliTool.AppMap) || !downloadService.isDownloaded(CliTool.Scanner)) {
            LOG.debug("Skipping launch of processes because tools are unavailable");
            return;
        }

        var contentRoots = ProjectRootManager.getInstance(project).getContentRoots();
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            var service = AppLandCommandLineService.getInstance();
            for (var contentRoot : contentRoots) {
                try {
                    if (!service.isRunning(contentRoot, false)) {
                        service.start(contentRoot);
                    }
                } catch (ExecutionException e) {
                    LOG.error("Error launch CLI tools for directory: " + contentRoot.getPath(), e);
                }
            }
        });
    }

    /**
     * Refresh the cli processes whenever the project roots change
     */
    public static void listenForContentRootChanges(@NotNull Project project, @NotNull Disposable disposable) {
        var busConnection = project.getMessageBus().connect(disposable);
        busConnection.subscribe(ProjectTopics.PROJECT_ROOTS, new ModuleRootListener() {
            @Override
            public void rootsChanged(@NotNull ModuleRootEvent event) {
                LOG.debug("rootsChanged: " + event);
                ApplicationManager.getApplication().executeOnPooledThread(() -> {
                    AppLandCommandLineService.getInstance().refreshForOpenProjects();
                });
            }
        });
    }
}
