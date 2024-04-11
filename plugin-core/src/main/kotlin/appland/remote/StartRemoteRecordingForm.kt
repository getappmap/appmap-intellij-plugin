package appland.remote

import appland.AppMapBundle
import com.intellij.ui.TextFieldWithHistory
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import javax.swing.JPanel

class StartRemoteRecordingForm(recentUrls: List<String>) {
    lateinit var urlComboBox: TextFieldWithHistory
        private set

    val url: String
        get() {
            return urlComboBox.text
        }

    val mainPanel: JPanel = panel {
        row(label = AppMapBundle.get("appMapRemoteRecording.urlLabel")) {
            urlComboBox = textFieldWithHistory(recentUrls)
                .align(AlignX.FILL)
                .focused()
                .component
        }
        row {
            comment(AppMapBundle.get("appMapRemoteRecording.message"))
        }
    }
}

