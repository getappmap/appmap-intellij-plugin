package appland.startup;

import appland.AppMapPlugin;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
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

        if (AppMapPlugin.getDescriptor().equals(pluginDescriptor)) {
            ApplicationManager.getApplication().invokeLater(() -> {
                FirstAppMapLaunchStartupActivity.handleFirstStart(project);
            }, ModalityState.defaultModalityState());
        }
    }
}
