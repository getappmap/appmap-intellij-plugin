package appland.settings;

import appland.upload.AppMapUploader;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

class AppMapProjectSettingsForm {
    // form designer
    private JPanel mainPanel;
    private JBTextField serverName;
    private JBCheckBox confirmUpload;
    private JBCheckBox enableFindings;
    private JBCheckBox enableTelemetry;

    AppMapProjectSettingsForm() {
        serverName.getEmptyText().setText(AppMapUploader.DEFAULT_SERVER_URL);
    }

    JPanel getMainPanel() {
        return mainPanel;
    }

    void loadSettingsFrom(@NotNull AppMapProjectSettings settings,
                          @NotNull AppMapApplicationSettings applicationSettings) {
        serverName.setText(settings.getCloudServerUrl());
        confirmUpload.setSelected(settings.getConfirmAppMapUpload());

        enableFindings.setSelected(applicationSettings.isEnableFindings());
        enableTelemetry.setSelected(applicationSettings.isEnableTelemetry());
    }

    void applySettingsTo(@NotNull AppMapProjectSettings settings,
                         @NotNull AppMapApplicationSettings applicationSettings,
                         boolean notify) {
        // project
        settings.setCloudServerUrl(StringUtil.nullize(serverName.getText()));
        settings.setConfirmAppMapUpload(confirmUpload.isSelected());

        // application
        applicationSettings.setEnableTelemetry(enableTelemetry.isSelected());
        if (notify) {
            applicationSettings.setEnableFindingsNotifying(enableFindings.isSelected());
        } else {
            applicationSettings.setEnableFindings(enableFindings.isSelected());
        }
    }
}
