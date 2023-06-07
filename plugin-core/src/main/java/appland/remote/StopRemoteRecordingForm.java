package appland.remote;

import appland.AppMapBundle;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.TextFieldWithHistory;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

public class StopRemoteRecordingForm {
    private JPanel mainPanel;
    private TextFieldWithHistory urlComboBox;
    private JBLabel urlLabel;
    private JBTextField appMapName;
    private JBLabel appMapNameLabel;

    public StopRemoteRecordingForm(@Nullable String activeRecordingUrl, @NotNull List<String> recentRecordingUrls) {
        urlLabel.setText(AppMapBundle.get("appMapRemoteRecording.urlLabel"));
        appMapNameLabel.setText(AppMapBundle.get("appMapRemoteRecording.appMapNameLabel"));

        urlComboBox.setHistory(recentRecordingUrls);
        if (StringUtil.isNotEmpty(activeRecordingUrl)) {
            urlComboBox.setText(activeRecordingUrl);
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

    public JBTextField getAppMapNameInput() {
        return appMapName;
    }

    public TextFieldWithHistory getUrlComboBox() {
        return urlComboBox;
    }
}
