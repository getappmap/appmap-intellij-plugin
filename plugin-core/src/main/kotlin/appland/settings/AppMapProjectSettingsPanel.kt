package appland.settings

import appland.AppMapBundle
import appland.actions.SetConfigurationUrlAction
import appland.cli.CliTool
import appland.deployment.AppMapDeploymentSettingsService.getCachedDeploymentSettings
import appland.deployment.Entitlement
import appland.enterpriseConfig.EnterpriseConfigService
import com.intellij.execution.configuration.EnvironmentVariablesComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.util.text.Strings
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.MutableCollectionComboBoxModel
import com.intellij.ui.JBIntSpinner
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.listCellRenderer.textListCellRenderer
import com.intellij.util.text.DateFormatUtil
import org.jetbrains.annotations.TestOnly
import java.awt.BorderLayout
import javax.swing.JCheckBox
import javax.swing.JEditorPane
import javax.swing.JLabel
import javax.swing.JPanel

class AppMapProjectSettingsPanel(private val project: Project?) {
    private val autoUpdateTools = DeploymentBackedSetting(
        messagePrefix = "projectSettings.automaticToolsUpdate",
        builtInDefault = true,
    ) { getCachedDeploymentSettings().autoUpdateTools }

    private val scanner = DeploymentBackedSetting(
        messagePrefix = "projectSettings.enableScanner",
        builtInDefault = false,
    ) { getCachedDeploymentSettings().scannerEnabled }

    private lateinit var enableTelemetry: JCheckBox
    private lateinit var cliEnvironment: EnvironmentVariablesComponent
    private lateinit var maxPinnedFileSizeKB: JBIntSpinner
    private lateinit var openAIKey: JBTextField
    private lateinit var useAnimation: JCheckBox
    private lateinit var appmapManifestUrl: JBTextField
    private lateinit var scannerManifestUrl: JBTextField
    private lateinit var orgConfigApplyRow: Row
    private lateinit var orgConfigStatusRow: Row
    private lateinit var orgConfigSourceRow: Row
    private lateinit var orgConfigStatusLabel: JLabel
    private lateinit var orgConfigSourceLabel: JLabel
    private lateinit var customerIdRow: Row
    private lateinit var customerIdLabel: JLabel

    @TestOnly
    fun getAutoUpdateToolsComboBox(): ComboBox<Boolean?> = autoUpdateTools.comboBox

    @TestOnly
    fun getScannerComboBox(): ComboBox<Boolean?> = scanner.comboBox

    @TestOnly
    fun getAutoUpdateToolsDeploymentComment(): String? = autoUpdateTools.visibleCommentText()

    @TestOnly
    fun getScannerDeploymentComment(): String? = scanner.visibleCommentText()

    @TestOnly
    fun getCustomerIdText(): String? = customerIdLabel.takeIf { it.isVisible }?.text

    /**
     * Reflects whether an organization configuration is currently applied. When applied, shows the
     * status row (with Change/Clear) plus a separate source line; when not, shows a single Apply button.
     * The source is on its own line and middle-truncated (full value in the tooltip) so a long URL
     * can't widen the dialog or trigger a horizontal scrollbar.
     */
    private fun updateOrgConfigStatus() {
        val service = EnterpriseConfigService.getInstance()
        val applied = service.isApplied
        // A URL source gets its own (truncated) line so a long URL can't widen the dialog; a local
        // file has no useful detail to show, so it's folded into the status line instead.
        var showSourceLine = false
        if (applied) {
            val appliedAt = AppMapApplicationSettingsService.getInstance().orgConfigAppliedAt
            val appliedSuffix = appliedAt?.let {
                AppMapBundle.get("projectSettings.orgConfig.appliedAt", DateFormatUtil.formatDateTime(it))
            } ?: ""

            if (service.resolveConfigUrl() != null) {
                orgConfigStatusLabel.text = AppMapBundle.get("projectSettings.orgConfig.active") + appliedSuffix
                val source = service.configSourceDescription ?: ""
                orgConfigSourceLabel.text = StringUtil.trimMiddle(source, 72)
                orgConfigSourceLabel.toolTipText = if (source.length > 72) source else null
                showSourceLine = true
            } else {
                orgConfigStatusLabel.text = AppMapBundle.get("projectSettings.orgConfig.activeLocalFile") + appliedSuffix
            }
        }
        orgConfigStatusRow.visible(applied)
        orgConfigSourceRow.visible(showSourceLine)
        orgConfigApplyRow.visible(!applied)
    }

    /**
     * The customer ID is only shown while the deployment is entitled. Entitlement can be gained or lost from
     * the buttons above, so this is recomputed on every reload rather than fixed at build time.
     */
    private fun updateCustomerId() {
        val customerId = Entitlement.getCustomerId()
        customerIdLabel.text = customerId ?: ""
        customerIdRow.visible(customerId != null)
    }

    fun loadSettingsFrom(
        applicationSettings: AppMapApplicationSettings,
        secureApplicationSettings: AppMapSecureApplicationSettings
    ) {
        enableTelemetry.isSelected = applicationSettings.isEnableTelemetry

        scanner.loadFrom(applicationSettings.enableScanner)

        cliEnvironment.envs = applicationSettings.cliEnvironment
        cliEnvironment.isPassParentEnvs = applicationSettings.isCliPassParentEnv
        maxPinnedFileSizeKB.number = applicationSettings.maxPinnedFileSizeKB

        openAIKey.text = Strings.notNullize(secureApplicationSettings.openAIKey)
        useAnimation.isSelected = applicationSettings.isUseAnimation

        autoUpdateTools.loadFrom(applicationSettings.autoUpdateTools)

        appmapManifestUrl.text = DownloadSettings.getManifestUrl(CliTool.AppMap)
        scannerManifestUrl.text = DownloadSettings.getManifestUrl(CliTool.Scanner)
        updateOrgConfigStatus()
        updateCustomerId()
    }

    fun applySettingsTo(
        applicationSettings: AppMapApplicationSettings,
        secureApplicationSettings: AppMapSecureApplicationSettings,
        notify: Boolean,
    ) {
        applicationSettings.isEnableTelemetry = enableTelemetry.isSelected

        val enableScannerValue = scanner.valueToStore()
        if (notify) {
            applicationSettings.setEnableScannerNotifying(enableScannerValue)
        } else {
            applicationSettings.enableScanner = enableScannerValue
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

        val autoUpdateToolsValue = autoUpdateTools.valueToStore()
        if (notify) {
            applicationSettings.setAutoUpdateToolsNotifying(autoUpdateToolsValue)
        } else {
            applicationSettings.autoUpdateTools = autoUpdateToolsValue
        }
        
        val defaultAppMapUrl = getCachedDeploymentSettings().appmapManifestUrl?.takeUnless { it.isBlank() } ?: DownloadSettings.DEFAULT_APPMAP_MANIFEST_URL
        val defaultScannerUrl = getCachedDeploymentSettings().scannerManifestUrl?.takeUnless { it.isBlank() } ?: DownloadSettings.DEFAULT_SCANNER_MANIFEST_URL
        
        val appmapManifest = appmapManifestUrl.text.takeIf { it.isNotBlank() && it != defaultAppMapUrl }
        val scannerManifest = scannerManifestUrl.text.takeIf { it.isNotBlank() && it != defaultScannerUrl }
        
        if (notify) {
            applicationSettings.setAppmapManifestUrlNotifying(appmapManifest)
            applicationSettings.setScannerManifestUrlNotifying(scannerManifest)
        } else {
            applicationSettings.appmapManifestUrl = appmapManifest
            applicationSettings.scannerManifestUrl = scannerManifest
        }
    }

    fun getMainPanel(): JPanel {
        cliEnvironment = EnvironmentVariablesComponent()
        cliEnvironment.labelLocation = BorderLayout.WEST

        val mainPanel = panel {
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
                autoUpdateTools.buildRow(this)
                scanner.buildRow(this)
                row(AppMapBundle.get("projectSettings.openAIKey.title")) {
                    openAIKey = textField().align(AlignX.FILL).component
                }.layout(RowLayout.INDEPENDENT)
                row {
                    cell(cliEnvironment).align(AlignX.FILL)
                }
            }
            group(AppMapBundle.get("projectSettings.advanced")) {
                // Shown when no organization configuration is applied.
                orgConfigApplyRow = row {
                    button(AppMapBundle.get("projectSettings.orgConfig.apply")) {
                        SetConfigurationUrlAction.showPicker(project)
                        updateOrgConfigStatus()
                    }
                }

                // Shown when an organization configuration is applied: status + change/clear.
                orgConfigStatusRow = row {
                    orgConfigStatusLabel = label("").component
                    button(AppMapBundle.get("projectSettings.orgConfig.change")) {
                        SetConfigurationUrlAction.showPicker(project)
                        updateOrgConfigStatus()
                    }
                    button(AppMapBundle.get("projectSettings.orgConfig.clear")) {
                        EnterpriseConfigService.getInstance().clearOrgConfig()
                        updateOrgConfigStatus()
                    }
                }
                // Source on its own line (middle-truncated) so a long URL doesn't widen the dialog.
                orgConfigSourceRow = row {
                    orgConfigSourceLabel = label("").component
                }

                // Read-only on purpose: the customer ID is set by an administrator through the bundled or
                // organization configuration, and there is deliberately no user-settable equivalent.
                customerIdRow = row(AppMapBundle.get("projectSettings.customerId.title")) {
                    customerIdLabel = label("").component
                }.rowComment(AppMapBundle.get("projectSettings.customerId.comment"))

                row(AppMapBundle.get("projectSettings.appmapManifestUrl.title")) {
                    appmapManifestUrl = textField().align(AlignX.FILL).component
                }.layout(RowLayout.INDEPENDENT)

                row(AppMapBundle.get("projectSettings.scannerManifestUrl.title")) {
                    scannerManifestUrl = textField().align(AlignX.FILL).component
                }.layout(RowLayout.INDEPENDENT)
            }
        }

        // The row must not be left visible-but-empty until the first reload, so it is initialised here — the
        // same way DeploymentBackedSetting populates itself from buildRow().
        updateCustomerId()

        return mainPanel
    }
}
/**
 * A tri-state setting which can fall back to the bundled/organization deployment configuration:
 * Yes / No / "Default from deployment settings".
 *
 * The `null` ("Default from deployment settings") entry, and the comment naming the deployment value, are only
 * present while the deployment configuration actually sets this field. That can change while the settings page
 * is open — an organization configuration is applied and cleared from the buttons in the panel's own "Advanced"
 * group — so both are recomputed by [loadFrom], which is what [AppMapProjectConfigurable.reset] calls when it's
 * notified of the change. There is deliberately no separate code path for building the row: [buildRow]
 * populates it through the same [refresh] the reload uses, so the two can't drift apart.
 *
 * @param messagePrefix Resource bundle prefix owning the `.title`, `.enabled`, `.disabled`,
 *                      `.deploymentDefault` and `.deploymentDefaultComment` keys.
 * @param builtInDefault The effective value when there is neither a user override nor a deployment default.
 * @param deploymentDefault The value the deployment configuration sets for this field, or `null` if it sets none.
 */
private class DeploymentBackedSetting(
    private val messagePrefix: String,
    private val builtInDefault: Boolean,
    private val deploymentDefault: () -> Boolean?,
) {
    private val model = MutableCollectionComboBoxModel<Boolean?>()

    lateinit var comboBox: ComboBox<Boolean?>
        private set

    private var comment: JEditorPane? = null

    fun buildRow(panel: Panel) {
        panel.row(AppMapBundle.get("$messagePrefix.title")) {
            // The comment is always created, even when there's nothing to say yet: it can't be added later,
            // and the deployment configuration may gain a default for this field while the page is open.
            val cell = comboBox(model, textListCellRenderer {
                when (it) {
                    null -> AppMapBundle.get("$messagePrefix.deploymentDefault")
                    else -> valueLabel(it)
                }
            }).comment("")

            comboBox = cell.component
            comment = cell.comment
        }

        refresh()
    }

    /**
     * Shows [userOverride], or the effective default when the user hasn't chosen anything.
     */
    fun loadFrom(userOverride: Boolean?) {
        refresh()

        comboBox.selectedItem = when {
            // without a deployment default, "no override" collapses to the built-in default
            userOverride == null && deploymentDefault() == null -> builtInDefault
            // with one, null is a selectable value meaning "use the deployment default"
            else -> userOverride
        }
    }

    /**
     * @return The value to persist as the user override, or `null` for "no override".
     */
    fun valueToStore(): Boolean? = when {
        // Storing the built-in default as an explicit override would be indistinguishable from a deliberate
        // choice, and would then win over a deployment default applied later.
        comboBox.selectedItem == builtInDefault && deploymentDefault() == null -> null
        else -> comboBox.selectedItem as? Boolean
    }

    /**
     * @return The deployment-default comment, or `null` while it isn't shown. Test-only.
     */
    fun visibleCommentText(): String? = comment?.takeIf { it.isVisible }?.text

    /**
     * Recomputes what the deployment configuration currently offers for this field.
     */
    private fun refresh() {
        val default = deploymentDefault()

        model.replaceAll(if (default == null) listOf(true, false) else listOf(null, true, false))

        comment?.let {
            it.isVisible = default != null
            if (default != null) {
                it.text = AppMapBundle.get("$messagePrefix.deploymentDefaultComment", valueLabel(default))
            }
        }
    }

    private fun valueLabel(value: Boolean): String =
        AppMapBundle.get(if (value) "$messagePrefix.enabled" else "$messagePrefix.disabled")
}
