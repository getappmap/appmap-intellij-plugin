package appland.startup;

import appland.settings.AppMapApplicationSettingsService;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
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

        if (AppMapApplicationSettingsService.getInstance().isFirstStart()) {
            AppMapApplicationSettingsService.getInstance().setFirstStart(false);
            AppLandStartupActivity.openToolWindowAndQuickstart(project);
        }
    }
}
