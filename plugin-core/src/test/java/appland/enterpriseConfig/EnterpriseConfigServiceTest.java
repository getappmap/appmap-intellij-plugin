package appland.enterpriseConfig;

import appland.AppMapBaseTest;
import appland.deployment.AppMapDeploymentSettings;
import appland.deployment.AppMapDeploymentSettingsService;
import appland.settings.AppMapApplicationSettingsService;
import com.google.gson.JsonObject;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.testFramework.fixtures.TempDirTestFixture;
import com.intellij.testFramework.fixtures.impl.TempDirTestFixtureImpl;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class EnterpriseConfigServiceTest extends AppMapBaseTest {
    @Override
    protected TempDirTestFixture createTempDirTestFixture() {
        return new TempDirTestFixtureImpl();
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        AppMapDeploymentSettingsService.reset();
        EnterpriseConfigService.getInstance().reset();
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            AppMapDeploymentSettingsService.reset();
            EnterpriseConfigService.getInstance().reset();
        } finally {
            super.tearDown();
        }
    }

    // --- resolveConfigUrl ---

    @Test
    public void resolveConfigUrl_nullWhenNothingConfigured() {
        assertNull(EnterpriseConfigService.getInstance().resolveConfigUrl());
    }

    @Test
    public void resolveConfigUrl_fromSettings() {
        AppMapApplicationSettingsService.getInstance().setConfigurationUrl("https://example.com/config");
        assertEquals("https://example.com/config", EnterpriseConfigService.getInstance().resolveConfigUrl());
    }

    // --- isApplied / markApplied ---

    @Test
    public void isApplied_falseByDefault() {
        assertFalse(EnterpriseConfigService.getInstance().isApplied());
    }

    @Test
    public void isApplied_trueWhenUrlConfigured() {
        AppMapApplicationSettingsService.getInstance().setConfigurationUrl("https://example.com/config");
        assertTrue(EnterpriseConfigService.getInstance().isApplied());
    }

    @Test
    public void isApplied_trueAfterMarkApplied() {
        EnterpriseConfigService.getInstance().markApplied();
        assertTrue(EnterpriseConfigService.getInstance().isApplied());
    }

    @Test
    public void markApplied_setsOrgConfigAppliedAt() {
        assertNull(AppMapApplicationSettingsService.getInstance().getOrgConfigAppliedAt());
        EnterpriseConfigService.getInstance().markApplied();
        assertNotNull(AppMapApplicationSettingsService.getInstance().getOrgConfigAppliedAt());
    }

    // --- applyLocalFile ---

    @Test
    public void applyLocalFile_parsesAndAppliesDeploymentSettings() {
        EnterpriseConfigService.getInstance().applyLocalFile("{\"appMap.autoUpdateTools\": false}", null);

        // enterprise settings override bundled; getCachedDeploymentSettings merges both
        var merged = AppMapDeploymentSettingsService.getCachedDeploymentSettings();
        assertEquals(Boolean.FALSE, merged.getAutoUpdateTools());
    }

    @Test
    public void applyLocalFile_marksApplied() {
        EnterpriseConfigService.getInstance().applyLocalFile("{}", null);
        assertTrue(EnterpriseConfigService.getInstance().isApplied());
    }

    @Test
    public void applyLocalFile_writesLocalFileSentinelCache() {
        EnterpriseConfigService.getInstance().applyLocalFile("{}", null);
        var cache = AppMapApplicationSettingsService.getInstance().getEnterpriseConfigCache();
        assertNotNull(cache);
        assertTrue("Cache must contain local-file sentinel", cache.contains("appmap:local-file"));
    }

    @Test
    public void applyLocalFile_invalidJson_leavesSettingsUntouched() {
        EnterpriseConfigService.getInstance().applyLocalFile("!!! not JSON !!!", null);
        assertFalse("Must not mark applied for invalid JSON", EnterpriseConfigService.getInstance().isApplied());
        assertNull("Enterprise config cache must stay null", AppMapApplicationSettingsService.getInstance().getEnterpriseConfigCache());
        assertNull("Enterprise deployment settings must stay null",
                AppMapDeploymentSettingsService.getInstance().getEnterpriseDeploymentSettings());
    }

    @Test
    public void applyLocalFile_nullJsonObject_leavesSettingsUntouched() {
        // Gson parses "null" as a null object, which the service rejects
        EnterpriseConfigService.getInstance().applyLocalFile("null", null);
        assertFalse(EnterpriseConfigService.getInstance().isApplied());
        assertNull(AppMapDeploymentSettingsService.getInstance().getEnterpriseDeploymentSettings());
    }

    @Test
    public void applyLocalFile_telemetryFieldParsed() {
        var json = """
                {
                  "appMap.telemetry": {
                    "backend": "splunk",
                    "url": "https://splunk.example.com:443",
                    "token": "tok",
                    "ca": "system"
                  }
                }
                """;
        EnterpriseConfigService.getInstance().applyLocalFile(json, null);

        var enterprise = AppMapDeploymentSettingsService.getInstance().getEnterpriseDeploymentSettings();
        assertNotNull(enterprise);
        assertNotNull(enterprise.getTelemetry());
        assertEquals("splunk", enterprise.getTelemetry().getBackend());
    }

    // --- fetch via file:// URL ---

    @Test
    public void fetchViaFileUrl_appliesDeploymentSettings() throws Exception {
        var configFile = writeTempConfig("{\"appMap.autoUpdateTools\": false}");
        AppMapApplicationSettingsService.getInstance().setConfigurationUrl("file://" + configFile);

        var latch = new CountDownLatch(1);
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            EnterpriseConfigService.awaitInitialFetchIfConfigured();
            latch.countDown();
        });
        assertTrue("Config must be applied within timeout", latch.await(5, TimeUnit.SECONDS));

        // Clear URL before asserting from EDT to avoid the EDT-with-active-URL guard
        AppMapApplicationSettingsService.getInstance().setConfigurationUrl(null);

        var enterprise = AppMapDeploymentSettingsService.getInstance().getEnterpriseDeploymentSettings();
        assertNotNull("Enterprise settings must be populated after file:// fetch", enterprise);
        assertEquals(Boolean.FALSE, enterprise.getAutoUpdateTools());
    }

    @Test
    public void fetchViaFileUrl_marksApplied() throws Exception {
        var configFile = writeTempConfig("{\"appMap.autoUpdateTools\": true}");
        AppMapApplicationSettingsService.getInstance().setConfigurationUrl("file://" + configFile);

        var latch = new CountDownLatch(1);
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            EnterpriseConfigService.awaitInitialFetchIfConfigured();
            latch.countDown();
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));

        assertTrue(EnterpriseConfigService.getInstance().isApplied());
    }

    @Test
    public void fetchViaFileUrl_writesCache() throws Exception {
        var json = "{\"appMap.autoUpdateTools\": false}";
        var configFile = writeTempConfig(json);
        var url = "file://" + configFile;
        AppMapApplicationSettingsService.getInstance().setConfigurationUrl(url);

        var latch = new CountDownLatch(1);
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            EnterpriseConfigService.awaitInitialFetchIfConfigured();
            latch.countDown();
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));

        var cache = AppMapApplicationSettingsService.getInstance().getEnterpriseConfigCache();
        assertNotNull(cache);
        assertTrue("Cache must contain the configured URL", cache.contains(url));
    }

    // --- persisted cache ---

    @Test
    public void persistedCache_localFileSentinelAppliedWithoutUrl() {
        // Simulate restart after a local-file apply: cache has sentinel, no URL configured
        var json = "{\"appMap.autoUpdateTools\": false}";
        AppMapApplicationSettingsService.getInstance().setEnterpriseConfigCache(buildCacheJson("appmap:local-file", json));

        // Safe from EDT because url is null — returns early after applyPersistedCache
        EnterpriseConfigService.awaitInitialFetchIfConfigured();

        var enterprise = AppMapDeploymentSettingsService.getInstance().getEnterpriseDeploymentSettings();
        assertNotNull("Local-file sentinel cache must be applied on startup", enterprise);
        assertEquals(Boolean.FALSE, enterprise.getAutoUpdateTools());
    }

    @Test
    public void persistedCache_appliedOnceOnly() {
        var json = "{\"appMap.autoUpdateTools\": false}";
        AppMapApplicationSettingsService.getInstance().setEnterpriseConfigCache(buildCacheJson("appmap:local-file", json));

        // First call — applies cache
        EnterpriseConfigService.awaitInitialFetchIfConfigured();
        assertNotNull(AppMapDeploymentSettingsService.getInstance().getEnterpriseDeploymentSettings());

        // Manually wipe enterprise settings to prove the next call does NOT re-apply
        AppMapDeploymentSettingsService.getInstance().setEnterpriseDeploymentSettings(null);

        // Second call — must be a no-op (cacheApplied gate is already true)
        EnterpriseConfigService.awaitInitialFetchIfConfigured();

        assertNull("Cache must not be re-applied after the one-shot gate fires",
                AppMapDeploymentSettingsService.getInstance().getEnterpriseDeploymentSettings());
    }

    @Test
    public void persistedCache_notAppliedWhenUrlMismatch() {
        var json = "{\"appMap.autoUpdateTools\": false}";
        // Cache was for a different URL than what's currently configured
        AppMapApplicationSettingsService.getInstance().setEnterpriseConfigCache(
                buildCacheJson("https://old.example.com/config", json));

        // Safe from EDT since url is null → early return after cache (mismatch → no-op)
        EnterpriseConfigService.awaitInitialFetchIfConfigured();

        assertNull("Cache with mismatched URL must not be applied",
                AppMapDeploymentSettingsService.getInstance().getEnterpriseDeploymentSettings());
    }

    // --- merge ---

    @Test
    public void merge_enterpriseOverridesBundledAutoUpdateTools() {
        AppMapDeploymentSettingsService.getInstance().setEnterpriseDeploymentSettings(
                new AppMapDeploymentSettings(null, false, null, null));

        var merged = AppMapDeploymentSettingsService.getCachedDeploymentSettings();
        assertEquals("Enterprise autoUpdateTools=false must override bundled null", Boolean.FALSE, merged.getAutoUpdateTools());
    }

    @Test
    public void merge_nullEnterpriseFieldFallsToBundled() {
        // Enterprise has all null fields — should not mask bundled values
        AppMapDeploymentSettingsService.getInstance().setEnterpriseDeploymentSettings(
                new AppMapDeploymentSettings());

        var merged = AppMapDeploymentSettingsService.getCachedDeploymentSettings();
        // In test environment there's no site-config.json, so bundled autoUpdateTools is also null
        assertNull("Null enterprise field must fall through to bundled (also null here)", merged.getAutoUpdateTools());
    }

    @Test
    public void merge_noEnterpriseSettingsUsesBundledDirectly() {
        // No enterprise settings at all → bundled returned
        var merged = AppMapDeploymentSettingsService.getCachedDeploymentSettings();
        // Bundled has autoUpdateTools=null in test environment (no site-config.json)
        assertNull(merged.getAutoUpdateTools());
    }

    @Test
    public void merge_enterpriseManifestUrlOverridesBundled() {
        var manifestUrl = "https://enterprise.example.com/manifest.json";
        AppMapDeploymentSettingsService.getInstance().setEnterpriseDeploymentSettings(
                new AppMapDeploymentSettings(null, null, manifestUrl, null));

        var merged = AppMapDeploymentSettingsService.getCachedDeploymentSettings();
        assertEquals("Enterprise appmapManifestUrl must override bundled null", manifestUrl, merged.getAppmapManifestUrl());
    }

    // --- helpers ---

    private Path writeTempConfig(String content) throws IOException {
        var path = Path.of(myFixture.getTempDirPath()).resolve("enterprise-config.json");
        Files.writeString(path, content);
        return path;
    }

    private static String buildCacheJson(String url, String innerJson) {
        var obj = new JsonObject();
        obj.addProperty("url", url);
        obj.addProperty("json", innerJson);
        return obj.toString();
    }
}
