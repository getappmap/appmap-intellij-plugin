package appland.actions;

import appland.AppMapBaseTest;
import appland.AppMapDeploymentTestUtils;
import appland.deployment.AppMapDeploymentSettings;
import appland.deployment.AppMapDeploymentTelemetrySettings;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import org.junit.Test;

public class PluginStatusTest extends AppMapBaseTest {
    @Test
    public void reportWithDeployment() throws Exception {
        var telemetrySettings = new AppMapDeploymentTelemetrySettings("splunk",
                "https://my-splunk.example.com:443",
                "my-hec-token",
                "my-ca-cert");
        AppMapDeploymentTestUtils.withSiteConfigFile(new AppMapDeploymentSettings(telemetrySettings), () -> {
            var report = PluginStatus.statusReportText(new EmptyProgressIndicator());
            assertNotNull(report);
        });
    }
}
