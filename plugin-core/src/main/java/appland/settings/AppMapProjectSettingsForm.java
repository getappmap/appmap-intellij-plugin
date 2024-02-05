package appland.settings;

import com.intellij.ui.components.JBCheckBox;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

class AppMapProjectSettingsForm {
    // form designer
    private JPanel mainPanel;
    private JBCheckBox enableTelemetry;

    AppMapProjectSettingsForm() {
    }

    JPanel getMainPanel() {
        return mainPanel;
    }

    void loadSettingsFrom(@NotNull AppMapProjectSettings settings,
                          @NotNull AppMapApplicationSettings applicationSettings) {
        enableTelemetry.setSelected(applicationSettings.isEnableTelemetry());
    }

    void applySettingsTo(@NotNull AppMapProjectSettings settings,
                         @NotNull AppMapApplicationSettings applicationSettings,
                         boolean notify) {
        // application settings
        applicationSettings.setEnableTelemetry(enableTelemetry.isSelected());
    }
}
