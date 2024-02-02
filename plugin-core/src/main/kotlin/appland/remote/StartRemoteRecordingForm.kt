package appland.remote

import appland.AppMapBundle
import com.intellij.ui.TextFieldWithHistory
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import javax.swing.JPanel

@Suppress("UnstableApiUsage")
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
                    .horizontalAlign(HorizontalAlign.FILL)
                    .focused()
                    .component
        }
        row {
            comment(AppMapBundle.get("appMapRemoteRecording.message"))
        }
    }
}

