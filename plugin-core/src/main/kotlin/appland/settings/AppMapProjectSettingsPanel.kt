package appland.settings

import appland.AppMapBundle
import com.intellij.execution.configuration.EnvironmentVariablesComponent
import com.intellij.openapi.util.text.Strings
import com.intellij.ui.JBIntSpinner
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.panel
import java.awt.BorderLayout
import javax.swing.JCheckBox
import javax.swing.JPanel

class AppMapProjectSettingsPanel {
    private lateinit var enableTelemetry: JCheckBox
    private lateinit var enableScanner: JCheckBox
    private lateinit var cliEnvironment: EnvironmentVariablesComponent
    private lateinit var maxPinnedFileSizeKB: JBIntSpinner
    private lateinit var openAIKey: JBTextField
    private lateinit var useAnimation: JCheckBox

    fun loadSettingsFrom(
        applicationSettings: AppMapApplicationSettings,
        secureApplicationSettings: AppMapSecureApplicationSettings
    ) {
        enableTelemetry.isSelected = applicationSettings.isEnableTelemetry
        enableScanner.isSelected = applicationSettings.isEnableScanner
        cliEnvironment.envs = applicationSettings.cliEnvironment
        cliEnvironment.isPassParentEnvs = applicationSettings.isCliPassParentEnv
        maxPinnedFileSizeKB.number = applicationSettings.maxPinnedFileSizeKB

        openAIKey.text = Strings.notNullize(secureApplicationSettings.openAIKey)
        useAnimation.isSelected = applicationSettings.isUseAnimation

    }

    fun applySettingsTo(
        applicationSettings: AppMapApplicationSettings,
        secureApplicationSettings: AppMapSecureApplicationSettings,
        notify: Boolean,
    ) {
        applicationSettings.isEnableTelemetry = enableTelemetry.isSelected
        if (notify) {
            applicationSettings.setEnableScannerNotifying(enableScanner.isSelected)
        } else {
            applicationSettings.isEnableScanner = enableScanner.isSelected
        }
        applicationSettings.isCliPassParentEnv = cliEnvironment.isPassParentEnvs
        if (notify) {
            applicationSettings.setCliEnvironmentNotifying(cliEnvironment.envs)
        } else {
            applicationSettings.cliEnvironment = cliEnvironment.envs
        }
        applicationSettings.maxPinnedFileSizeKB = maxPinnedFileSizeKB.number

        secureApplicationSettings.openAIKey = Strings.nullize(openAIKey.text)
        applicationSettings.isUseAnimation = useAnimation.isSelected
    }

    fun getMainPanel(): JPanel {
        cliEnvironment = EnvironmentVariablesComponent()
        cliEnvironment.labelLocation = BorderLayout.WEST

        return panel {
            row {
                enableTelemetry = checkBox(AppMapBundle.get("projectSettings.enableTelemetry.title")).component
            }
            row(AppMapBundle.get("projectSettings.maxPinnedFileSize.label")) {
                maxPinnedFileSizeKB = spinner(0..4096).gap(RightGap.SMALL).component
                label(AppMapBundle.get("projectSettings.maxPinnedFileSize.unit"))
                rowComment(AppMapBundle.get("projectSettings.maxPinnedFileSize.comment"))
            }
            row {
                useAnimation = checkBox(AppMapBundle.get("projectSettings.useAnimation.title")).component
            }
            group(AppMapBundle.get("projectSettings.appMapServices")) {
                row {
                    enableScanner = checkBox(AppMapBundle.get("projectSettings.enableScanner.title")).component
                }
                row(AppMapBundle.get("projectSettings.openAIKey.title")) {
                    openAIKey = textField().align(AlignX.FILL).component
                }.layout(RowLayout.INDEPENDENT)
                row {
                    cell(cliEnvironment).align(AlignX.FILL)
                }
            }
        }
    }
}