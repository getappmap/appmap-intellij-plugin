package appland.remote

import appland.AppMapBundle
import com.intellij.ui.TextFieldWithHistory
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel

class StopRemoteRecordingForm(activeRecordingUrl: String?, recentUrls: List<String>) {
    lateinit var urlComboBox: TextFieldWithHistory
        private set
    lateinit var appMapNameInput: JBTextField
        private set

    val mainPanel = panel {
        row(AppMapBundle.get("appMapRemoteRecording.urlLabel")) {
            urlComboBox = textFieldWithHistory(recentUrls).align(AlignX.FILL).component
            if (!activeRecordingUrl.isNullOrEmpty()) {
                urlComboBox.text = activeRecordingUrl
            }
        }

        row(AppMapBundle.get("appMapRemoteRecording.appMapNameLabel")) {
            appMapNameInput = textField().align(AlignX.FILL).component
        }
    }

    val url: String
        get() {
            return urlComboBox.text
        }

    val name: String
        get() {
            return appMapNameInput.text
        }
}