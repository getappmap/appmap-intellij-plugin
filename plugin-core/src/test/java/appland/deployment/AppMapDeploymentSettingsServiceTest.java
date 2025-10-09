package appland.deployment;

import appland.AppMapBaseTest;
import appland.AppMapPlugin;
import appland.cli.CliTool;
import appland.cli.CliTools;
import com.intellij.testFramework.fixtures.TempDirTestFixture;
import com.intellij.testFramework.fixtures.impl.TempDirTestFixtureImpl;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static appland.AppMapDeploymentTestUtils.withSiteConfigFile;

public class AppMapDeploymentSettingsServiceTest extends AppMapBaseTest {
    @Override
    protected TempDirTestFixture createTempDirTestFixture() {
        // create temp files on disk
        return new TempDirTestFixtureImpl();
    }

    @Test
    public void defaultStatus() {
        assertTrue(
                "By default, downloads must be permitted",
                AppMapDeploymentSettingsService.getCachedDeploymentSettings().isAutoUpdateTools()
        );
    }

    @Test
    public void bundledFilesSelection() throws IOException {
        var baseDir = Path.of(myFixture.getTempDirPath()).resolve("bundledBinaries");
        Files.createDirectories(baseDir);

        createMockBinaries(baseDir, CliTool.AppMap, 0, "win", "x64", ".exe");
        createMockBinaries(baseDir, CliTool.AppMap, 1, "win", "arm64", ".exe");
        createMockBinaries(baseDir, CliTool.AppMap, 2, "linux", "x64", "");
        createMockBinaries(baseDir, CliTool.AppMap, 3, "linux", "arm64", "");
        createMockBinaries(baseDir, CliTool.AppMap, 4, "macos", "x64", "");
        createMockBinaries(baseDir, CliTool.AppMap, 5, "macos", "arm64", "");

        createMockBinaries(baseDir, CliTool.Scanner, 0, "win", "x64", ".exe");
        createMockBinaries(baseDir, CliTool.Scanner, 1, "win", "arm64", ".exe");
        createMockBinaries(baseDir, CliTool.Scanner, 2, "linux", "x64", "");
        createMockBinaries(baseDir, CliTool.Scanner, 3, "linux", "arm64", "");
        createMockBinaries(baseDir, CliTool.Scanner, 4, "macos", "x64", "");
        createMockBinaries(baseDir, CliTool.Scanner, 5, "macos", "arm64", "");

        assertBinaryPath("appmap-win-x64-0.11.0.exe", baseDir, CliTool.AppMap, "win", "x64");
        assertBinaryPath("appmap-win-arm64-1.11.0.exe", baseDir, CliTool.AppMap, "win", "arm64");
        assertBinaryPath("appmap-linux-x64-2.11.0", baseDir, CliTool.AppMap, "linux", "x64");
        assertBinaryPath("appmap-linux-arm64-3.11.0", baseDir, CliTool.AppMap, "linux", "arm64");
        assertBinaryPath("appmap-macos-x64-4.11.0", baseDir, CliTool.AppMap, "macos", "x64");
        assertBinaryPath("appmap-macos-arm64-5.11.0", baseDir, CliTool.AppMap, "macos", "arm64");

        assertBinaryPath("scanner-win-x64-0.11.0.exe", baseDir, CliTool.Scanner, "win", "x64");
        assertBinaryPath("scanner-win-arm64-1.11.0.exe", baseDir, CliTool.Scanner, "win", "arm64");
        assertBinaryPath("scanner-linux-x64-2.11.0", baseDir, CliTool.Scanner, "linux", "x64");
        assertBinaryPath("scanner-linux-arm64-3.11.0", baseDir, CliTool.Scanner, "linux", "arm64");
        assertBinaryPath("scanner-macos-x64-4.11.0", baseDir, CliTool.Scanner, "macos", "x64");
        assertBinaryPath("scanner-macos-arm64-5.11.0", baseDir, CliTool.Scanner, "macos", "arm64");
    }

    @Test
    public void deploymentConfigurationParsing() throws Exception {
        var content = """
                {
                  "appMap.telemetry": {
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
                  "appMap.telemetry": {
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

    private void createMockBinaries(@NotNull Path baseDir,
                                    @NotNull CliTool type,
                                    int majorVersion,
                                    @NotNull String platform,
                                    @NotNull String arch,
                                    @NotNull String suffix) throws IOException {
        var prefix = type.getId();
        Files.createFile(baseDir.resolve("%s-%s-%s-%d.1.0%s".formatted(prefix, platform, arch, majorVersion, suffix)));
        Files.createFile(baseDir.resolve("%s-%s-%s-%d.2.0%s".formatted(prefix, platform, arch, majorVersion, suffix)));
        Files.createFile(baseDir.resolve("%s-%s-%s-%d.10.0%s".formatted(prefix, platform, arch, majorVersion, suffix)));
        Files.createFile(baseDir.resolve("%s-%s-%s-%d.11.0%s".formatted(prefix, platform, arch, majorVersion, suffix)));
    }

    private static void assertBinaryPath(@NotNull String expected,
                                         @NotNull Path baseDir,
                                         @NotNull CliTool type,
                                         @NotNull String platform,
                                         @NotNull String arch) {
        var appmapBinaries = AppMapDeploymentSettingsService.findBundledBinaries(baseDir, type, platform, arch);
        assertNotNull(appmapBinaries);
        var appmapBinary = appmapBinaries.stream().max(CliTools.pathComparator).orElse(null);
        assertNotNull(appmapBinary);
        assertEquals(expected, appmapBinary.getFileName().toString());
    }
}