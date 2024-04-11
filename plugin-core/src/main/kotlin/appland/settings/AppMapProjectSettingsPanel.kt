package appland.settings

import appland.AppMapBundle
import com.intellij.execution.configuration.EnvironmentVariablesComponent
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import java.awt.BorderLayout
import javax.swing.JCheckBox
import javax.swing.JPanel

class AppMapProjectSettingsPanel {
    private lateinit var enableTelemetry: JCheckBox
    private lateinit var cliEnvironment: EnvironmentVariablesComponent

    fun loadSettingsFrom(applicationSettings: AppMapApplicationSettings) {
        enableTelemetry.isSelected = applicationSettings.isEnableTelemetry
        cliEnvironment.envs = applicationSettings.cliEnvironment
        cliEnvironment.isPassParentEnvs = applicationSettings.isCliPassParentEnv
    }

    fun applySettingsTo(applicationSettings: AppMapApplicationSettings) {
        applicationSettings.isEnableTelemetry = enableTelemetry.isSelected
        applicationSettings.cliEnvironment = cliEnvironment.envs
        applicationSettings.isCliPassParentEnv = cliEnvironment.isPassParentEnvs
    }

    fun getMainPanel(): JPanel {
        cliEnvironment = EnvironmentVariablesComponent()
        cliEnvironment.labelLocation = BorderLayout.WEST
        return panel {
            row {
                enableTelemetry = checkBox(AppMapBundle.get("projectSettings.enableTelemetry.title")).component
            }
            group(AppMapBundle.get("projectSettings.appMapServices")) {
                row {
                    cell(cliEnvironment).align(AlignX.FILL)
                }
            }
        }
    }
}