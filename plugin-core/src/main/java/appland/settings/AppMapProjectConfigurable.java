package appland.settings;

import appland.AppMapBundle;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.util.messages.MessageBusConnection;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

public class AppMapProjectConfigurable implements Configurable {
    private final AppMapProjectSettingsPanel form;
    private final Project project;
    private @Nullable MessageBusConnection settingsConnection;

    public AppMapProjectConfigurable(@NotNull Project project) {
        this.project = project;
        this.form = new AppMapProjectSettingsPanel(project);
    }

    @Override
    public @NlsContexts.ConfigurableName String getDisplayName() {
        return AppMapBundle.get("projectSettings.displayName");
    }

    @Override
    public @Nullable JComponent createComponent() {
        var component = form.getMainPanel();

        // While the settings page is open, reload it when the organization configuration changes
        // (e.g. an applied URL finished fetching, or a background auto-update arrived) so the
        // displayed values reflect the effective settings immediately instead of only after reopen.
        // Defensively drop any previous connection: the platform usually pairs createComponent with
        // disposeUIResources, but if createComponent is called again first, don't leak the old listener.
        if (settingsConnection != null) {
            settingsConnection.disconnect();
        }
        var connection = ApplicationManager.getApplication().getMessageBus().connect();
        connection.subscribe(AppMapSettingsListener.TOPIC, new AppMapSettingsListener() {
            @Override
            public void enterpriseDeploymentSettingsChanged() {
                reloadOnEdt();
            }

            @Override
            public void autoUpdateToolsChanged() {
                reloadOnEdt();
            }
        });
        this.settingsConnection = connection;

        return component;
    }

    private void reloadOnEdt() {
        ApplicationManager.getApplication().invokeLater(this::reset, ModalityState.any());
    }

    @Override
    public void disposeUIResources() {
        if (settingsConnection != null) {
            settingsConnection.disconnect();
            settingsConnection = null;
        }
        Configurable.super.disposeUIResources();
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
    static class InlineSecureApplicationSettings implements AppMapSecureApplicationSettings {
        @NotNull private Map<String, String> modelConfig;

        public InlineSecureApplicationSettings() {
            this.modelConfig = new HashMap<>();
        }

        // copy constructor
        public InlineSecureApplicationSettings(@NotNull AppMapSecureApplicationSettings settings) {
            this.modelConfig = new HashMap<>(settings.getModelConfig());
        }

        public @Nullable String getOpenAIKey() {
            return modelConfig.get(AppMapSecureApplicationSettingsService.MODEL_CONFIG_OPENAI_API_KEY);
        }

        public void setOpenAIKey(@Nullable String value) {
            setModelConfigItem(AppMapSecureApplicationSettingsService.MODEL_CONFIG_OPENAI_API_KEY, value);
        }

        @Override
        public void setModelConfigItem(@NotNull String key, @Nullable String value) {
            if (value == null) {
                modelConfig.remove(key);
            } else {
                modelConfig.put(key, value);
            }
        }
    }
}
