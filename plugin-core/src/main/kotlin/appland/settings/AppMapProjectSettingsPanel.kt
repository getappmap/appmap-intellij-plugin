package appland.settings

import appland.AppMapBundle
import appland.deployment.AppMapDeploymentSettingsService.getCachedDeploymentSettings
import com.intellij.execution.configuration.EnvironmentVariablesComponent
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.util.text.Strings
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.JBIntSpinner
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.listCellRenderer.textListCellRenderer
import java.awt.BorderLayout
import javax.swing.JCheckBox
import javax.swing.JPanel

class AppMapProjectSettingsPanel {
    private lateinit var enableTelemetry: JCheckBox
    private lateinit var enableAutoToolsUpdate: ComboBox<Boolean?>
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

        val autoUpdateTools = applicationSettings.autoUpdateTools
        enableAutoToolsUpdate.selectedItem = when {
            // without deployment settings, null means to enable automatic downloads
            autoUpdateTools == null && getCachedDeploymentSettings().isEmpty -> true
            // with deployment settings, both true and false override the default from the deployment setting
            else -> autoUpdateTools
        }
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

        val autoUpdateTools = when {
            enableAutoToolsUpdate.selectedItem == true && getCachedDeploymentSettings().isEmpty -> null
            else -> enableAutoToolsUpdate.selectedItem as? Boolean
        }
        if (notify) {
            applicationSettings.setAutoUpdateToolsNotifying(autoUpdateTools)
        } else {
            applicationSettings.autoUpdateTools = autoUpdateTools
        }
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
                row(AppMapBundle.get("projectSettings.automaticToolsUpdate.title")) {
                    val deploymentSettings = getCachedDeploymentSettings()
                    val values = when {
                        deploymentSettings.isEmpty -> listOf(true, false)
                        // null to show "Use deployment settings"
                        else -> listOf(null, true, false)
                    }

                    comboBox(CollectionComboBoxModel(values), textListCellRenderer {
                        when (it) {
                            true -> AppMapBundle.get("projectSettings.automaticToolsUpdate.enabled")
                            false -> AppMapBundle.get("projectSettings.automaticToolsUpdate.disabled")
                            null -> AppMapBundle.get("projectSettings.automaticToolsUpdate.deploymentDefault")
                        }
                    }).apply {
                        if (!deploymentSettings.isEmpty) {
                            val value = when (deploymentSettings.isAutoUpdateTools) {
                                true -> AppMapBundle.get("projectSettings.automaticToolsUpdate.enabled")
                                false -> AppMapBundle.get("projectSettings.automaticToolsUpdate.disabled")
                            }
                            comment(AppMapBundle.get("projectSettings.automaticToolsUpdate.deploymentDefaultComment", value))
                        }
                    }.applyToComponent {
                        enableAutoToolsUpdate = this
                    }
                }
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