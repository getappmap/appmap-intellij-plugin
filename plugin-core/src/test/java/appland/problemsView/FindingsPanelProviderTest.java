package appland.problemsView;

import appland.AppMapBaseTest;
import appland.deployment.AppMapDeploymentSettings;
import appland.deployment.AppMapDeploymentSettingsService;
import appland.settings.AppMapApplicationSettingsService;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import org.junit.After;
import org.junit.Test;

/**
 * Quiescent processes produce no new findings, but a stale {@code appmap-findings.json} would still populate the
 * Problems View "Runtime Analysis" tab. It's the one findings surface which isn't behind the tool window swap,
 * so it needs its own gate.
 */
public class FindingsPanelProviderTest extends AppMapBaseTest {
    @Test
    public void tabUnavailableWhileSignedOut() {
        var settings = AppMapApplicationSettingsService.getInstance();
        settings.setApiKey(null);
        settings.setEnableScanner(true);

        assertNull("The Runtime Analysis tab must be unavailable while signed out",
                new FindingsPanelProvider(getProject()).create());
    }

    @Test
    public void tabUnavailableWithDisabledScanner() {
        var settings = AppMapApplicationSettingsService.getInstance();
        settings.setApiKey("test-api-key");
        settings.setEnableScanner(false);

        assertNull("The Runtime Analysis tab must be unavailable if the scanner is disabled",
                new FindingsPanelProvider(getProject()).create());
    }

    @After
    public void resetDeploymentSettings() {
        AppMapDeploymentSettingsService.reset();
    }

    @Test
    public void tabAvailableWithEnabledScannerAndAnEntitlement() {
        var settings = AppMapApplicationSettingsService.getInstance();
        settings.setApiKey(null);
        settings.setEnableScanner(true);
        AppMapDeploymentSettingsService.getInstance().setEnterpriseDeploymentSettings(
                AppMapDeploymentSettings.builder().customerId("acme-corp").build());

        var tab = new FindingsPanelProvider(getProject()).create();
        try {
            assertNotNull("An entitled deployment must get the Runtime Analysis tab without a session", tab);
        } finally {
            if (tab instanceof Disposable) {
                Disposer.dispose((Disposable) tab);
            }
        }
    }

    @Test
    public void tabAvailableWithEnabledScannerAndSignedIn() {
        var settings = AppMapApplicationSettingsService.getInstance();
        settings.setApiKey("test-api-key");
        settings.setEnableScanner(true);

        var tab = new FindingsPanelProvider(getProject()).create();
        try {
            assertNotNull("The Runtime Analysis tab must be available if the scanner is enabled and the user signed in", tab);
        } finally {
            if (tab instanceof Disposable) {
                Disposer.dispose((Disposable) tab);
            }
        }
    }
}
