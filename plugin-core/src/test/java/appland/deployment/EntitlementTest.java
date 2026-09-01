package appland.deployment;

import appland.AppMapBaseTest;
import appland.enterpriseConfig.EnterpriseConfigService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Test;

import static appland.AppMapDeploymentTestUtils.withSiteConfigFile;

/**
 * Entitlement is the single read path for the managed customer ID. It is deliberately not a licence check:
 * an unverified ID, set by administrators through the channels they already use, is a business-process
 * boundary rather than a security one.
 */
public class EntitlementTest extends AppMapBaseTest {
    @After
    public void resetConfigurationState() {
        // applyLocalFile() writes the organization-config cache, so clean up both layers we touched
        AppMapDeploymentSettingsService.reset();
        EnterpriseConfigService.getInstance().reset();
    }

    @Test
    public void notEntitledWithoutAnyConfiguration() {
        assertNull(Entitlement.getCustomerId());
        assertFalse("A standard build with no organization config is not entitled", Entitlement.isEntitled());
    }

    @Test
    public void entitledByBundledConfiguration() throws Exception {
        withSiteConfigFile(customerIdConfig("acme-corp"), () -> {
            assertEquals("acme-corp", Entitlement.getCustomerId());
            assertTrue(Entitlement.isEntitled());
        });
    }

    @Test
    public void entitledByOrganizationConfiguration() {
        applyOrganizationConfig(customerIdConfig("acme-corp"));

        assertEquals("acme-corp", Entitlement.getCustomerId());
        assertTrue(Entitlement.isEntitled());
    }

    @Test
    public void customerIdIsTrimmed() {
        applyOrganizationConfig(customerIdConfig("  acme-corp\n"));

        assertEquals("acme-corp", Entitlement.getCustomerId());
    }

    @Test
    public void blankCustomerIdReadsAsUnset() {
        applyOrganizationConfig(customerIdConfig("   "));

        assertNull("Whitespace-only must read as unset", Entitlement.getCustomerId());
        assertFalse(Entitlement.isEntitled());
    }

    /**
     * A standard build installed over a bundled one: the {@code site-config.json} is gone and there is no
     * organization config, so entitlement lapses. There is no tombstone to keep it alive.
     */
    @Test
    public void standardBuildOverABundledOneIsNotEntitled() throws Exception {
        withSiteConfigFile(customerIdConfig("acme-corp"), () -> assertTrue(Entitlement.isEntitled()));

        // withSiteConfigFile removes the file again, which is exactly the standard-build state
        assertFalse("Without the bundled site-config.json the plugin is no longer entitled",
                Entitlement.isEntitled());
    }

    @Test
    public void organizationEntitlementSurvivesWithoutABundledConfiguration() {
        applyOrganizationConfig(customerIdConfig("acme-corp"));

        assertTrue("The organization layer is independent of which build is installed",
                Entitlement.isEntitled());
    }

    /**
     * The JSON is administrator-authored, so a value of the wrong type must not entitle. An object where a
     * string is expected makes the whole document invalid, and the existing rejection path covers it.
     */
    @Test
    public void malformedCustomerIdValueDoesNotEntitle() {
        EnterpriseConfigService.getInstance().applyLocalFile("{\"appMap.customerId\": {\"a\": 1}}", null);

        assertNull(Entitlement.getCustomerId());
        assertFalse(Entitlement.isEntitled());
    }

    private static @NotNull AppMapDeploymentSettings customerIdConfig(@Nullable String customerId) {
        return AppMapDeploymentSettings.builder().customerId(customerId).build();
    }

    private void applyOrganizationConfig(@NotNull AppMapDeploymentSettings settings) {
        AppMapDeploymentSettingsService.getInstance().setEnterpriseDeploymentSettings(settings);
    }
}
