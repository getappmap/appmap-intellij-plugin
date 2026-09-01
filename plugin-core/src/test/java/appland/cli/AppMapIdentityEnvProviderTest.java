package appland.cli;

import appland.AppMapBaseTest;
import appland.deployment.AppMapDeploymentSettings;
import appland.deployment.AppMapDeploymentSettingsService;
import appland.settings.AppMapApplicationSettingsService;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Test;

import java.util.Map;

import static appland.AppMapDeploymentTestUtils.withSiteConfigFile;

/**
 * The two AppMap identity variables are independent: a session token authenticates, a customer ID attributes.
 * Neither is faked in the absence of the other — the CLI has to be able to tell the two states apart.
 */
public class AppMapIdentityEnvProviderTest extends AppMapBaseTest {
    private static final String API_KEY = "APPMAP_API_KEY";
    private static final String CUSTOMER_ID = "APPMAP_CUSTOMER_ID";

    @After
    public void resetDeploymentSettings() {
        AppMapDeploymentSettingsService.reset();
    }

    @Test
    public void neitherASessionNorAnEntitlement() {
        signOut();

        assertEquals("Nothing to say about identity means no variables at all", Map.of(), environment());
    }

    @Test
    public void sessionOnly() {
        signIn();

        assertEquals("api-key", environment().get(API_KEY));
        assertFalse("Without an entitlement the variable must be absent, not empty",
                environment().containsKey(CUSTOMER_ID));
    }

    @Test
    public void entitlementOnly() throws Exception {
        signOut();

        withSiteConfigFile(customerIdConfig("acme-corp"), () -> {
            assertEquals("acme-corp", environment().get(CUSTOMER_ID));
            assertFalse("An entitled deployment has no session token, so the variable must be absent",
                    environment().containsKey(API_KEY));
        });
    }

    /**
     * Both are passed when both are present. The CLI contract — the real key authenticates, the customer ID is
     * attribution only — is the CLI's to implement.
     */
    @Test
    public void sessionAndEntitlement() throws Exception {
        signIn();

        withSiteConfigFile(customerIdConfig("acme-corp"), () -> {
            assertEquals("api-key", environment().get(API_KEY));
            assertEquals("acme-corp", environment().get(CUSTOMER_ID));
        });
    }

    @Test
    public void blankCustomerIdOmitsTheVariable() throws Exception {
        signOut();

        withSiteConfigFile(customerIdConfig("   "), () -> assertFalse("A blank customer ID must not reach the subprocess as an empty variable",
                environment().containsKey(CUSTOMER_ID)));
    }

    private static @NotNull Map<String, String> environment() {
        return new AppMapIdentityEnvProvider().getEnvironment();
    }

    private static @NotNull AppMapDeploymentSettings customerIdConfig(@NotNull String customerId) {
        return AppMapDeploymentSettings.builder().customerId(customerId).build();
    }

    private static void signIn() {
        AppMapApplicationSettingsService.getInstance().setApiKey("api-key");
    }

    private static void signOut() {
        AppMapApplicationSettingsService.getInstance().setApiKey(null);
    }
}
