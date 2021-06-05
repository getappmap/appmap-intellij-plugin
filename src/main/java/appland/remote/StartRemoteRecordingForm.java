package appland.remote;

import appland.AppMapBundle;
import com.intellij.ui.TextFieldWithHistory;
import com.intellij.ui.components.JBLabel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;

public class StartRemoteRecordingForm {
    private JPanel mainPanel;
    private TextFieldWithHistory urlComboBox;
    private JBLabel infoLabel;
    private JBLabel urlLabel;

    public StartRemoteRecordingForm(@NotNull List<String> recentURLs) {
        urlLabel.setText(AppMapBundle.get("appMapRemoteRecording.urlLabel"));
        infoLabel.setText(AppMapBundle.get("appMapRemoteRecording.message"));
        urlComboBox.setHistory(recentURLs);
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    @NotNull
    public String getURL() {
        return this.urlComboBox.getText();
    }

    public TextFieldWithHistory getUrlComboBox() {
        return urlComboBox;
    }
}
