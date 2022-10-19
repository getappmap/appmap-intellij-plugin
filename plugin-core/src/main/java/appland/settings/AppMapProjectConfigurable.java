package appland.settings;

import appland.AppMapBundle;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class AppMapProjectConfigurable implements Configurable {
    private final AppMapProjectSettingsForm form = new AppMapProjectSettingsForm();
    private final Project project;

    public AppMapProjectConfigurable(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public @NlsContexts.ConfigurableName String getDisplayName() {
        return AppMapBundle.get("projectSettings.displayName");
    }

    @Override
    public @Nullable JComponent createComponent() {
        return form.getMainPanel();
    }

    @Override
    public void reset() {
        var projectSettings = AppMapProjectSettingsService.getState(project);
        var applicationSettings = AppMapApplicationSettingsService.getInstance();
        form.loadSettingsFrom(projectSettings, applicationSettings);
    }

    @Override
    public boolean isModified() {
        var settings = AppMapProjectSettingsService.getState(project);
        var applicationSettings = AppMapApplicationSettingsService.getInstance();

        var newSettings = new AppMapProjectSettings(settings);
        var newApplicationSettings = new AppMapApplicationSettings(applicationSettings);
        form.applySettingsTo(newSettings, newApplicationSettings);

        return !settings.equals(newSettings) || !applicationSettings.equals(newApplicationSettings);
    }

    @Override
    public void apply() throws ConfigurationException {
        var projectSettings = AppMapProjectSettingsService.getState(project);
        var applicationSettings = AppMapApplicationSettingsService.getInstance();
        form.applySettingsTo(projectSettings, applicationSettings);
    }
}
