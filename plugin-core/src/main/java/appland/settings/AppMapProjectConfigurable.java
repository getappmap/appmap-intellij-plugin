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
    private final AppMapProjectSettingsPanel form = new AppMapProjectSettingsPanel();
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
        form.loadSettingsFrom(AppMapApplicationSettingsService.getInstance());
    }

    @Override
    public boolean isModified() {
        var settings = AppMapProjectSettingsService.getState(project);
        var applicationSettings = AppMapApplicationSettingsService.getInstance();

        var newSettings = new AppMapProjectSettings(settings);
        var newApplicationSettings = new AppMapApplicationSettings(applicationSettings);
        form.applySettingsTo(newApplicationSettings);

        return !settings.equals(newSettings) || !applicationSettings.equals(newApplicationSettings);
    }

    @Override
    public void apply() throws ConfigurationException {
        form.applySettingsTo(AppMapApplicationSettingsService.getInstance());
    }
}
