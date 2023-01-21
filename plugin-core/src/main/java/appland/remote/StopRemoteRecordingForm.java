package appland.remote;

import appland.AppMapBundle;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.TextFieldWithHistory;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.SystemDependent;

import javax.swing.*;
import java.util.List;

public class StopRemoteRecordingForm {
    private JPanel mainPanel;
    private TextFieldWithHistory urlComboBox;
    private JBLabel urlLabel;
    private JBTextField appMapName;
    private JBLabel appMapNameLabel;
    private JBLabel appMapLocation;
    private TextFieldWithBrowseButton directoryLocationInput;

    public StopRemoteRecordingForm(@NotNull Project project,
                                   @Nullable String defaultStorageLocation,
                                   @Nullable String activeRecordingUrl,
                                   @NotNull List<String> recentRecordingUrls) {
        urlLabel.setText(AppMapBundle.get("appMapRemoteRecording.urlLabel"));
        appMapNameLabel.setText(AppMapBundle.get("appMapRemoteRecording.appMapNameLabel"));
        appMapLocation.setText(AppMapBundle.get("appMapRemoteRecording.locationLabel"));

        urlComboBox.setHistory(recentRecordingUrls);
        if (StringUtil.isNotEmpty(activeRecordingUrl)) {
            urlComboBox.setText(activeRecordingUrl);
        }

        directoryLocationInput.addBrowseFolderListener(AppMapBundle.get("action.stopAppMapRemoteRecording.fileChooserTitle"),
                null, project, new FileChooserDescriptor(false, true, false, false, false, false));
        if (StringUtil.isNotEmpty(defaultStorageLocation)) {
            directoryLocationInput.setText(defaultStorageLocation);
        }
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    @NotNull
    public String getURL() {
        return this.urlComboBox.getText();
    }

    @NotNull
    public String getName() {
        return this.appMapName.getText();
    }

    @NotNull
    @SystemDependent
    public String getDirectoryLocation() {
        return this.directoryLocationInput.getText();
    }

    public JBTextField getAppMapNameInput() {
        return appMapName;
    }

    public TextFieldWithHistory getUrlComboBox() {
        return urlComboBox;
    }

    public TextFieldWithBrowseButton getDirectoryLocationInput() {
        return directoryLocationInput;
    }
}
