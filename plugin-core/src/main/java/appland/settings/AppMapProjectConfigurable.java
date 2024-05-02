package appland.settings;

import appland.AppMapBundle;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import lombok.Data;
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
        form.loadSettingsFrom(AppMapApplicationSettingsService.getInstance(), AppMapSecureApplicationSettingsService.getInstance());
    }

    @Override
    public boolean isModified() {
        var settings = AppMapProjectSettingsService.getState(project);
        var applicationSettings = AppMapApplicationSettingsService.getInstance();
        var secureApplicationSettings = new InlineSecureApplicationSettings(AppMapSecureApplicationSettingsService.getInstance());

        var newSettings = new AppMapProjectSettings(settings);
        var newApplicationSettings = new AppMapApplicationSettings(applicationSettings);
        var newSecureApplicationSettings = new InlineSecureApplicationSettings();
        form.applySettingsTo(newApplicationSettings, newSecureApplicationSettings, false);

        return !settings.equals(newSettings)
                || !applicationSettings.equals(newApplicationSettings)
                || !secureApplicationSettings.equals(newSecureApplicationSettings);
    }

    @Override
    public void apply() {
        form.applySettingsTo(
                AppMapApplicationSettingsService.getInstance(),
                AppMapSecureApplicationSettingsService.getInstance(),
                true);
    }

    /**
     * Secure settings which are not persisted to implement {@link #isModified()}.
     */
    @Data
    private static class InlineSecureApplicationSettings implements AppMapSecureApplicationSettings {
        String openAIKey;

        public InlineSecureApplicationSettings() {
        }

        // copy constructor
        public InlineSecureApplicationSettings(@NotNull AppMapSecureApplicationSettings settings) {
            this.openAIKey = settings.getOpenAIKey();
        }
    }
}
