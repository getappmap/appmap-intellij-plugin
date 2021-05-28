package appland.remote;

import appland.AppMapBundle;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.TextFieldWithHistory;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.NotNull;
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
    private TextFieldWithBrowseButton saveLocationInput;

    public StopRemoteRecordingForm(@NotNull Project project, @NotNull List<String> recentURLs, @NotNull String lastLocation) {
        urlLabel.setText(AppMapBundle.get("appMapRemoteRecording.urlLabel"));
        urlComboBox.setHistory(recentURLs);

        saveLocationInput.addBrowseFolderListener(AppMapBundle.get("action.stopAppMapRemoteRecording.fileChooserTitle"),
                null, project, new FileChooserDescriptor(true, false, false, false, false, false));
        if (!lastLocation.isBlank()) {
            saveLocationInput.setText(lastLocation);
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
    public String getLocation() {
        return this.saveLocationInput.getText();
    }

    public JBTextField getAppMapNameInput() {
        return appMapName;
    }

    public TextFieldWithHistory getUrlComboBox() {
        return urlComboBox;
    }

    public TextFieldWithBrowseButton getSaveLocationInput() {
        return saveLocationInput;
    }
}
