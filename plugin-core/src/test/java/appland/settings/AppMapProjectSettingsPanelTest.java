package appland.settings;

import appland.AppMapBaseTest;
import appland.AppMapBundle;
import appland.deployment.AppMapDeploymentSettings;
import appland.deployment.AppMapDeploymentSettingsService;
import appland.deployment.AppMapDeploymentTelemetrySettings;
import appland.enterpriseConfig.EnterpriseConfigService;
import com.intellij.openapi.ui.ComboBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static appland.AppMapDeploymentTestUtils.withSiteConfigFile;

/**
 * The "Use deployment settings" combo box entry, and the comment naming that default, must be offered per
 * field. Keying them on "are there any deployment settings at all" made a configuration which only sets
 * telemetry claim a deployment default for auto-update and the scanner, which it does not have.
 */
public class AppMapProjectSettingsPanelTest extends AppMapBaseTest {
    private static final List<Boolean> WITHOUT_DEPLOYMENT_DEFAULT = Arrays.asList(true, false);
    private static final List<Boolean> WITH_DEPLOYMENT_DEFAULT = Arrays.asList(null, true, false);
    /** The label a deployment default of {@code true} is rendered with. */
    private static final String ENABLED_LABEL = AppMapBundle.get("projectSettings.enableScanner.enabled");

    @Before
    public void dropPersistedOrganizationConfig() {
        // a cache left behind by another test would be applied on the first read and mask what we set up here
        EnterpriseConfigService.getInstance().reset();
    }

    @After
    public void resetDeploymentSettings() {
        AppMapDeploymentSettingsService.reset();
    }

    @Test
    public void telemetryOnlyConfigurationOffersNoDeploymentDefaults() throws Exception {
        var telemetryOnly = AppMapDeploymentSettings.builder()
                .telemetry(new AppMapDeploymentTelemetrySettings(
                        "splunk", "https://splunk.example.com:443", "token", "system"))
                .build();

        withSiteConfigFile(telemetryOnly, () -> {
            var panel = createPanel();

            assertEquals("A telemetry-only configuration must not claim an auto-update default",
                    WITHOUT_DEPLOYMENT_DEFAULT, itemsOf(panel.getAutoUpdateToolsComboBox()));
            assertEquals("A telemetry-only configuration must not claim a scanner default",
                    WITHOUT_DEPLOYMENT_DEFAULT, itemsOf(panel.getScannerComboBox()));

            assertNull("No comment may name an auto-update default which doesn't exist",
                    panel.getAutoUpdateToolsDeploymentComment());
            assertNull("No comment may name a scanner default which doesn't exist",
                    panel.getScannerDeploymentComment());
        });
    }

    @Test
    public void deploymentDefaultIsOfferedPerField() throws Exception {
        withSiteConfigFile(scannerOnlyDeploymentSettings(), () -> {
            var panel = createPanel();

            assertEquals("The scanner has a deployment default, so it must be offered",
                    WITH_DEPLOYMENT_DEFAULT, itemsOf(panel.getScannerComboBox()));
            assertEquals("Auto-update has no deployment default, so it must not be offered",
                    WITHOUT_DEPLOYMENT_DEFAULT, itemsOf(panel.getAutoUpdateToolsComboBox()));

            assertDeploymentComment(ENABLED_LABEL, panel.getScannerDeploymentComment());
            assertNull("Auto-update has no deployment default to name",
                    panel.getAutoUpdateToolsDeploymentComment());
        });
    }

    @Test
    public void loadCollapsesNoOverrideToTheBuiltInDefaultPerField() throws Exception {
        withSiteConfigFile(scannerOnlyDeploymentSettings(), () -> {
            var panel = createPanel();

            // no user overrides at all
            panel.loadSettingsFrom(new AppMapApplicationSettings(),
                    new AppMapProjectConfigurable.InlineSecureApplicationSettings());

            assertEquals("Without a deployment default, no override must show the built-in default",
                    Boolean.TRUE, panel.getAutoUpdateToolsComboBox().getSelectedItem());
            assertNull("With a deployment default, no override must show \"Use deployment settings\"",
                    panel.getScannerComboBox().getSelectedItem());
        });
    }

    @Test
    public void applyStoresNoOverrideForTheBuiltInDefaultPerField() throws Exception {
        withSiteConfigFile(scannerOnlyDeploymentSettings(), () -> {
            var panel = createPanel();
            panel.getAutoUpdateToolsComboBox().setSelectedItem(true);
            panel.getScannerComboBox().setSelectedItem(true);

            var settings = new AppMapApplicationSettings();
            panel.applySettingsTo(settings, new AppMapProjectConfigurable.InlineSecureApplicationSettings(), false);

            assertNull("Choosing the built-in default without a deployment default must store no override",
                    settings.getAutoUpdateTools());
            assertEquals("An explicit choice overriding a deployment default must be stored",
                    Boolean.TRUE, settings.getEnableScanner());
        });
    }

    /**
     * The combo box values are built once, but an organization configuration can be applied or cleared while
     * this panel is open — from the buttons in its own "Advanced" group, no less.
     */
    @Test
    public void offeredValuesFollowAnAppliedOrganizationConfiguration() {
        var panel = createPanel();
        assertEquals("No deployment default before an organization configuration is applied",
                WITHOUT_DEPLOYMENT_DEFAULT, itemsOf(panel.getScannerComboBox()));
        assertNull(panel.getScannerDeploymentComment());

        applyOrganizationConfig(scannerOnlyDeploymentSettings());
        reload(panel);

        assertEquals("An applied organization configuration must be offered as the deployment default",
                WITH_DEPLOYMENT_DEFAULT, itemsOf(panel.getScannerComboBox()));
        assertSelectionIsOffered(panel.getScannerComboBox());
        assertDeploymentComment(ENABLED_LABEL, panel.getScannerDeploymentComment());
    }

    @Test
    public void offeredValuesFollowAClearedOrganizationConfiguration() {
        applyOrganizationConfig(scannerOnlyDeploymentSettings());

        var panel = createPanel();
        assertEquals(WITH_DEPLOYMENT_DEFAULT, itemsOf(panel.getScannerComboBox()));
        assertDeploymentComment(ENABLED_LABEL, panel.getScannerDeploymentComment());

        applyOrganizationConfig(null);
        reload(panel);

        assertEquals("A cleared organization configuration must no longer be offered as the deployment default",
                WITHOUT_DEPLOYMENT_DEFAULT, itemsOf(panel.getScannerComboBox()));
        assertSelectionIsOffered(panel.getScannerComboBox());
        assertNull("The comment must not keep naming a default which was cleared",
                panel.getScannerDeploymentComment());
    }

    private static void assertDeploymentComment(@NotNull String expectedValueLabel, @Nullable String comment) {
        assertNotNull("A deployment default must be named by a comment", comment);
        assertTrue("The comment must name the deployment value " + expectedValueLabel + ", but was: " + comment,
                comment.contains(expectedValueLabel));
    }

    private void applyOrganizationConfig(@Nullable AppMapDeploymentSettings settings) {
        AppMapDeploymentSettingsService.getInstance().setEnterpriseDeploymentSettings(settings);
    }

    /**
     * Does what {@link AppMapProjectConfigurable#reset()} does when it's notified that the organization
     * configuration changed while the settings page is open.
     */
    private void reload(@NotNull AppMapProjectSettingsPanel panel) {
        panel.loadSettingsFrom(new AppMapApplicationSettings(),
                new AppMapProjectConfigurable.InlineSecureApplicationSettings());
    }

    /**
     * A combo box must never display a value which isn't in its list of offered values: it looks selected, but
     * the user can't select it again after changing away from it.
     */
    private static void assertSelectionIsOffered(@NotNull ComboBox<Boolean> comboBox) {
        var items = itemsOf(comboBox);
        assertTrue("The displayed value " + comboBox.getSelectedItem() + " must be offered, but only "
                        + items + " are",
                items.contains(comboBox.getSelectedItem()));
    }

    /**
     * A deployment configuration which only sets {@code appMap.scannerEnabled}.
     */
    private static @NotNull AppMapDeploymentSettings scannerOnlyDeploymentSettings() {
        return AppMapDeploymentSettings.builder().scannerEnabled(true).build();
    }

    private @NotNull AppMapProjectSettingsPanel createPanel() {
        var panel = new AppMapProjectSettingsPanel(getProject());
        // the combo boxes only exist after the UI was built
        panel.getMainPanel();
        return panel;
    }

    private static @NotNull List<Boolean> itemsOf(@NotNull ComboBox<Boolean> comboBox) {
        var model = comboBox.getModel();
        var items = new ArrayList<Boolean>(model.getSize());
        for (var i = 0; i < model.getSize(); i++) {
            items.add(model.getElementAt(i));
        }
        return items;
    }
}
