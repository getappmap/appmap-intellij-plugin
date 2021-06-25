package appland.settings;

import appland.upload.AppMapUploader;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

class AppMapProjectSettingsForm {
    // internal properties
    boolean confirmUploadModified = false;

    // form designer
    private JPanel mainPanel;
    private JBTextField serverName;
    private JBCheckBox confirmUpload;

    AppMapProjectSettingsForm() {
        serverName.getEmptyText().setText(AppMapUploader.DEFAULT_SERVER_URL);

        confirmUpload.addItemListener(e -> confirmUploadModified = true);
    }

    JPanel getMainPanel() {
        return mainPanel;
    }

    void loadSettingsFrom(@NotNull AppMapProjectSettings settings) {
        confirmUploadModified = false;

        serverName.setText(settings.getCloudServerUrl());
        confirmUpload.setSelected(settings.getConfirmAppMapUpload() == Boolean.TRUE);
    }

    void applySettingsTo(@NotNull AppMapProjectSettings settings) {
        settings.setCloudServerUrl(StringUtil.nullize(serverName.getText()));
        if (confirmUploadModified) {
            settings.setConfirmAppMapUpload(confirmUpload.isSelected());
        }
    }
}
