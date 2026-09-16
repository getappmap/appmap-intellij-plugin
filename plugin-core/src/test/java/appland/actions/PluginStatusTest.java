package appland.actions;

import appland.AppMapBaseTest;
import appland.AppMapDeploymentTestUtils;
import appland.deployment.AppMapDeploymentSettings;
import appland.deployment.AppMapDeploymentTelemetrySettings;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import org.junit.Test;

public class PluginStatusTest extends AppMapBaseTest {
    /**
     * The status report is the verification path for administrators rolling out a managed entitlement, so it
     * has to name the customer ID in full. Unlike the Splunk token it is not a secret.
     */
    @Test
    public void reportNamesTheCustomerIdWhenEntitled() throws Exception {
        AppMapDeploymentTestUtils.withSiteConfigFile(
                AppMapDeploymentSettings.builder().customerId("acme-corp").build(), () -> {
                    var report = PluginStatus.statusReportText(new EmptyProgressIndicator());

                    assertTrue("The report must state that the deployment is entitled:\n" + report,
                            report.contains("Managed entitlement: active"));
                    assertTrue("The report must name the customer ID:\n" + report,
                            report.contains("acme-corp"));
                });
    }

    @Test
    public void reportSaysNotEntitledWithoutACustomerId() {
        var report = PluginStatus.statusReportText(new EmptyProgressIndicator());

        assertTrue("The report must say so when there is no entitlement:\n" + report,
                report.contains("Managed entitlement: not configured"));
    }

    @Test
    public void reportWithDeployment() throws Exception {
        var telemetrySettings = new AppMapDeploymentTelemetrySettings("splunk",
                "https://my-splunk.example.com:443",
                "my-hec-token",
                "my-ca-cert");
        AppMapDeploymentTestUtils.withSiteConfigFile(AppMapDeploymentSettings.builder().telemetry(telemetrySettings).build(), () -> {
            var report = PluginStatus.statusReportText(new EmptyProgressIndicator());
            assertNotNull(report);
        });
    }
}
