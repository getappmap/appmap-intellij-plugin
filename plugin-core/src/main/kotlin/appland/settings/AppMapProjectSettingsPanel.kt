package appland.settings

import appland.AppMapBundle
import com.intellij.ui.dsl.builder.panel
import javax.swing.JCheckBox
import javax.swing.JPanel

@Suppress("UnstableApiUsage")
class AppMapProjectSettingsPanel {
    private lateinit var enableTelemetry: JCheckBox

    fun loadSettingsFrom(applicationSettings: AppMapApplicationSettings) {
        enableTelemetry.isSelected = applicationSettings.isEnableTelemetry
    }

    fun applySettingsTo(applicationSettings: AppMapApplicationSettings) {
        applicationSettings.isEnableTelemetry = enableTelemetry.isSelected
    }

    fun getMainPanel(): JPanel {
        return panel {
            row {
                enableTelemetry = checkBox(AppMapBundle.get("projectSettings.enableTelemetry.title")).component
            }
        }
    }
}