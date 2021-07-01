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

    AppMapProjectSettingsForm() {
        serverName.getEmptyText().setText(AppMapUploader.DEFAULT_SERVER_URL);
    }

    JPanel getMainPanel() {
        return mainPanel;
    }

    void loadSettingsFrom(@NotNull AppMapProjectSettings settings) {
        serverName.setText(settings.getCloudServerUrl());
        confirmUpload.setSelected(settings.getConfirmAppMapUpload());
    }

    void applySettingsTo(@NotNull AppMapProjectSettings settings) {
        settings.setCloudServerUrl(StringUtil.nullize(serverName.getText()));
        settings.setConfirmAppMapUpload(confirmUpload.isSelected());
    }
}
