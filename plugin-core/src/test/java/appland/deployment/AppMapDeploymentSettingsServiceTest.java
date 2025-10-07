package appland.deployment;

import appland.AppMapBaseTest;
import appland.AppMapPlugin;
import org.junit.Test;

import java.nio.file.Path;

import static appland.AppMapDeploymentTestUtils.withSiteConfigFile;

public class AppMapDeploymentSettingsServiceTest extends AppMapBaseTest {
    @Test
    public void deploymentConfigurationParsing() throws Exception {
        var content = """
                {
                  "appmap.telemetry": {
                    "backend": "splunk",
                    "url": "https://splunk.example.com:443",
                    "token": "my-hec-token",
                    "ca": "system"
                  }
                }
                """;

        withSiteConfigFile(Path.of(myFixture.getTempDirPath()), content, path -> {
            var parsedSettings = AppMapDeploymentSettingsService.readDeploymentSettings(path);
            assertNotNull(parsedSettings);

            AppMapDeploymentTelemetrySettings telemetry = parsedSettings.getTelemetry();
            assertNotNull(telemetry);
            assertEquals("splunk", telemetry.getBackend());
            assertEquals("https://splunk.example.com:443", telemetry.getUrl());
            assertEquals("my-hec-token", telemetry.getToken());
            assertEquals("system", telemetry.getCertificateAuthorityCertificate());
        });
    }

    @Test
    public void deploymentConfigurationAtTopLevel() throws Exception {
        var content = """
                {
                  "appmap.telemetry": {
                    "backend": "splunk",
                    "url": "https://splunk.example.com:443",
                    "token": "my-hec-token",
                    "ca": "system"
                  }
                }
                """;

        withSiteConfigFile(AppMapPlugin.getPluginPath(), content, path -> {
            var parsedSettings = AppMapDeploymentSettingsService.readDeploymentSettings();
            assertNotNull("Deployment settings must be read from the top-level of the plugin directory", parsedSettings);
            assertNotNull(parsedSettings.getTelemetry());
        });
    }

    @Test
    public void deploymentConfigurationNestedDirectory() throws Exception {
        var content = """
                {
                  "appmap.telemetry": {
                    "backend": "splunk",
                    "url": "https://splunk.example.com:443",
                    "token": "my-hec-token",
                    "ca": "system"
                  }
                }
                """;

        withSiteConfigFile(AppMapPlugin.getPluginPath().resolve("extension"), content, path -> {
            var parsedSettings = AppMapDeploymentSettingsService.readDeploymentSettings();
            assertNotNull("Deployment settings must be read from nested extensions dir of the plugin directory", parsedSettings);
            assertNotNull(parsedSettings.getTelemetry());
        });
    }
}