package appland.startup;

import appland.AppMapBundle;
import appland.AppMapPlugin;
import appland.cli.AppLandCommandLineService;
import appland.rpcService.AppLandJsonRpcService;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class DynamicPluginListener implements com.intellij.ide.plugins.DynamicPluginListener {
    private final Project project;

    public DynamicPluginListener(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public void pluginLoaded(@NotNull IdeaPluginDescriptor pluginDescriptor) {
        if (project.isDefault() || project.isDisposed()) {
            return;
        }

        if (isAppMapPlugin(pluginDescriptor)) {
            ApplicationManager.getApplication().invokeLater(() -> {
                FirstAppMapLaunchStartupActivity.handleFirstStart(project);
            }, ModalityState.defaultModalityState());
        }
    }

    @Override
    public void beforePluginUnload(@NotNull IdeaPluginDescriptor pluginDescriptor, boolean isUpdate) {
        if (isAppMapPlugin(pluginDescriptor)) {
            // Terminate AppMap processes before the plugin is unloaded.
            // If we did not terminate here, then it would happen in service disposable, which is executed in a WriteAction.
            // Waiting for termination in a WriteAction would freeze the IDE.

            // Execute under progress in a background thread to avoid blocking the UI
            ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
                AppLandCommandLineService.getInstance().stopAll(true);

                AppLandJsonRpcService.getInstance(project).stopServerAsync();
            }, AppMapBundle.get("appmap.unload.stoppingProcesses.title"), false, project);
        }
    }

    private static boolean isAppMapPlugin(@NotNull PluginDescriptor pluginDescriptor) {
        return AppMapPlugin.getDescriptor().equals(pluginDescriptor);
    }
}
