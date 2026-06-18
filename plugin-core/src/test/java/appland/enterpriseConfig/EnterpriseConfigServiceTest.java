package appland.enterpriseConfig;

import appland.AppMapBaseTest;
import appland.deployment.AppMapDeploymentSettings;
import appland.deployment.AppMapDeploymentSettingsService;
import appland.settings.AppMapApplicationSettingsService;
import appland.settings.AppMapSettingsListener;
import appland.telemetry.TelemetryService;
import appland.utils.GsonUtils;
import com.google.gson.JsonObject;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.testFramework.fixtures.TempDirTestFixture;
import com.intellij.testFramework.fixtures.impl.TempDirTestFixtureImpl;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
            // Some tests apply Splunk telemetry, which reloads the shared app-level telemetry reporter to
            // an always-enabled Splunk reporter. Rebuild it from the now-cleared settings so it doesn't
            // leak an "always enabled" state into other tests (e.g. TelemetryStatusEnvProviderTest).
            var telemetry = ApplicationManager.getApplication().getServiceIfCreated(TelemetryService.class);
            if (telemetry != null) {
                telemetry.reloadReporter();
            }
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
        var cache = EnterpriseConfigCacheService.getInstance().getCacheJson();
        assertNotNull(cache);
        assertTrue("Cache must contain local-file sentinel", cache.contains("appmap:local-file"));
    }

    @Test
    public void applyLocalFile_invalidJson_leavesSettingsUntouched() {
        EnterpriseConfigService.getInstance().applyLocalFile("!!! not JSON !!!", null);
        assertFalse("Must not mark applied for invalid JSON", EnterpriseConfigService.getInstance().isApplied());
        assertNull("Enterprise config cache must stay null", EnterpriseConfigCacheService.getInstance().getCacheJson());
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

    @Test
    public void applyLocalFile_supersedesUserOverrideForSpecifiedSetting() {
        var settings = AppMapApplicationSettingsService.getInstance();
        settings.setAutoUpdateTools(true); // user had explicitly enabled auto-update

        EnterpriseConfigService.getInstance().applyLocalFile("{\"appMap.autoUpdateTools\": false}", null);

        assertNull("User override must be cleared so the org config takes effect", settings.getAutoUpdateTools());
        assertEquals("Org config value must now be effective", Boolean.FALSE,
                AppMapDeploymentSettingsService.getCachedDeploymentSettings().getAutoUpdateTools());
    }

    @Test
    public void applyLocalFile_keepsUserOverrideForUnspecifiedSetting() {
        var settings = AppMapApplicationSettingsService.getInstance();
        settings.setAutoUpdateTools(true);

        // Config specifies a manifest URL but says nothing about autoUpdateTools
        EnterpriseConfigService.getInstance().applyLocalFile(
                "{\"appMap.manifest.appmapUrl\": \"https://example.com/manifest.json\"}", null);

        assertEquals("Override for a setting the org config doesn't specify must be kept",
                Boolean.TRUE, settings.getAutoUpdateTools());
    }

    @Test
    public void applyLocalFile_clearsExistingUrlSetting() {
        var settings = AppMapApplicationSettingsService.getInstance();
        settings.setConfigurationUrl("https://example.com/config");

        EnterpriseConfigService.getInstance().applyLocalFile("{\"appMap.autoUpdateTools\": false}", null);

        assertNull("Switching to a local file must clear the configured URL", settings.getConfigurationUrl());
        var cache = EnterpriseConfigCacheService.getInstance().getCacheJson();
        assertNotNull(cache);
        assertTrue("Cache must hold the local-file sentinel, not the old URL", cache.contains("appmap:local-file"));
    }

    // --- clearOrgConfig ---

    @Test
    public void clearOrgConfig_resetsAllState() throws Exception {
        AppMapApplicationSettingsService.getInstance().setConfigurationUrl("https://example.com/config");
        AppMapDeploymentSettingsService.getInstance().setEnterpriseDeploymentSettings(
                GsonUtils.GSON.fromJson(SPLUNK_JSON, AppMapDeploymentSettings.class));
        EnterpriseConfigCacheService.getInstance().setCacheJson("{\"url\":\"x\",\"json\":\"{}\"}");
        AppMapApplicationSettingsService.getInstance().setOrgConfigAppliedAt(1L);

        runOnPooledThreadAndWait(() -> EnterpriseConfigService.getInstance().clearOrgConfig());

        var settings = AppMapApplicationSettingsService.getInstance();
        assertNull(settings.getConfigurationUrl());
        assertNull(EnterpriseConfigCacheService.getInstance().getCacheJson());
        assertNull(settings.getOrgConfigAppliedAt());
        assertNull(AppMapDeploymentSettingsService.getInstance().getEnterpriseDeploymentSettings());
    }

    // --- telemetrySettingsChanged is only fired when telemetry actually changes ---

    @Test
    public void clearOrgConfig_firesTelemetryChangeWhenClearingSplunk() throws Exception {
        AppMapDeploymentSettingsService.getInstance().setEnterpriseDeploymentSettings(
                GsonUtils.GSON.fromJson(SPLUNK_JSON, AppMapDeploymentSettings.class));
        AppMapApplicationSettingsService.getInstance().setOrgConfigAppliedAt(1L);

        var telemetryChanges = subscribeTelemetryChanges();
        runOnPooledThreadAndWait(() -> EnterpriseConfigService.getInstance().clearOrgConfig());

        assertEquals("Clearing a Splunk org config changes telemetry, so the change must be fired",
                1, telemetryChanges.get());
    }

    @Test
    public void clearOrgConfig_withoutTelemetry_doesNotFireTelemetryChange() throws Exception {
        AppMapDeploymentSettingsService.getInstance().setEnterpriseDeploymentSettings(
                GsonUtils.GSON.fromJson("{\"appMap.autoUpdateTools\": false}", AppMapDeploymentSettings.class));
        AppMapApplicationSettingsService.getInstance().setOrgConfigAppliedAt(1L);

        var telemetryChanges = subscribeTelemetryChanges();
        runOnPooledThreadAndWait(() -> EnterpriseConfigService.getInstance().clearOrgConfig());

        assertEquals("Clearing a config with no telemetry must not fire a telemetry change (avoids a costly restart)",
                0, telemetryChanges.get());
    }

    @Test
    public void reapplyingIdenticalTelemetry_firesTelemetryChangeOnce() throws Exception {
        var telemetryChanges = subscribeTelemetryChanges();
        var deploymentChanges = subscribeDeploymentChanges();

        // Wait on the telemetry-change signal itself, not the deployment-change one: fireSettingsChanged
        // publishes enterpriseDeploymentSettingsChanged BEFORE telemetrySettingsChanged (with the reporter
        // reload in between), so waiting on the deployment counter can observe it a moment before the
        // telemetry counter is incremented.
        EnterpriseConfigService.getInstance().applyLocalFile(SPLUNK_JSON, null);
        waitUntil(() -> telemetryChanges.get() >= 1);
        assertEquals("Telemetry appearing for the first time must fire a change", 1, telemetryChanges.get());

        // Re-apply the exact same telemetry: the deployment-change event still fires, but telemetry didn't
        // change — so no further telemetry-change event. The second apply has no telemetry event to race,
        // so the deployment counter is a reliable "second apply finished" signal here.
        EnterpriseConfigService.getInstance().applyLocalFile(SPLUNK_JSON, null);
        waitUntil(() -> deploymentChanges.get() >= 2);
        assertEquals("Re-applying identical telemetry must not fire another telemetry change", 1, telemetryChanges.get());
    }

    // --- getAppliedConfigJson redaction (the Splunk token must not leak into the status report) ---

    @Test
    public void getAppliedConfigJson_redactsSplunkToken() {
        var json = """
                {
                  "appMap.telemetry": {
                    "backend": "splunk",
                    "url": "https://splunk.example.com:443",
                    "token": "super-secret-hec-token",
                    "ca": "system"
                  }
                }
                """;
        EnterpriseConfigService.getInstance().applyLocalFile(json, null);

        var applied = EnterpriseConfigService.getInstance().getAppliedConfigJson();
        assertNotNull(applied);
        assertFalse("Splunk HEC token must not appear in the status-report JSON",
                applied.contains("super-secret-hec-token"));
        // Non-secret fields are still shown so the report stays useful for debugging.
        assertTrue("Backend should still be shown", applied.contains("splunk"));
        assertTrue("URL should still be shown", applied.contains("splunk.example.com"));
        assertTrue("CA (public, not a secret) should still be shown", applied.contains("system"));
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

        var cache = EnterpriseConfigCacheService.getInstance().getCacheJson();
        assertNotNull(cache);
        assertTrue("Cache must contain the configured URL", cache.contains(url));
    }

    @Test
    public void sameUrlRefetch_supersedesOnlyChangedSettings() throws Exception {
        var settings = AppMapApplicationSettingsService.getInstance();
        var configFile = writeTempConfig(
                "{\"appMap.autoUpdateTools\": false, \"appMap.manifest.appmapUrl\": \"https://a.example.com/m.json\"}");
        settings.setConfigurationUrl("file://" + configFile);

        // First fetch establishes the cached config for this URL (no user overrides yet).
        runOnPooledThreadAndWait(EnterpriseConfigService::awaitInitialFetchIfConfigured);

        // User overrides both settings.
        settings.setAutoUpdateTools(true);
        settings.setAppmapManifestUrl("https://user.example.com/m.json");

        // Re-fetch the SAME URL with only the manifest URL changed (autoUpdateTools unchanged).
        Files.writeString(configFile,
                "{\"appMap.autoUpdateTools\": false, \"appMap.manifest.appmapUrl\": \"https://b.example.com/m.json\"}");
        runOnPooledThreadAndWait(EnterpriseConfigService::awaitInitialFetchIfConfigured);

        assertEquals("An unchanged org setting must NOT supersede the user override",
                Boolean.TRUE, settings.getAutoUpdateTools());
        assertNull("A changed org setting MUST supersede the user override", settings.getAppmapManifestUrl());
    }

    @Test
    public void interactiveFetchFailure_clearsPreviouslyAppliedConfig() throws Exception {
        // Start with a valid applied config.
        EnterpriseConfigService.getInstance().applyLocalFile("{\"appMap.autoUpdateTools\": false}", null);
        assertNotNull(AppMapDeploymentSettingsService.getInstance().getEnterpriseDeploymentSettings());

        // Interactively point at an unreachable URL.
        AppMapApplicationSettingsService.getInstance().setConfigurationUrl(
                "file://" + Path.of(myFixture.getTempDirPath()).resolve("does-not-exist.json"));
        EnterpriseConfigService.getInstance().applyAsync();

        // The interactive failure must clear the previously-applied config (rather than keep it stale).
        waitUntil(() -> AppMapDeploymentSettingsService.getInstance().getEnterpriseDeploymentSettings() == null);
        assertNull("Cache must be cleared on interactive failure",
                EnterpriseConfigCacheService.getInstance().getCacheJson());
    }

    // --- persisted cache ---

    @Test
    public void persistedCache_localFileSentinelAppliedWithoutUrl() {
        // Simulate restart after a local-file apply: cache has sentinel, no URL configured
        var json = "{\"appMap.autoUpdateTools\": false}";
        EnterpriseConfigCacheService.getInstance().setCacheJson(buildCacheJson("appmap:local-file", json));

        // Safe from EDT because url is null — returns early after applyPersistedCache
        EnterpriseConfigService.awaitInitialFetchIfConfigured();

        var enterprise = AppMapDeploymentSettingsService.getInstance().getEnterpriseDeploymentSettings();
        assertNotNull("Local-file sentinel cache must be applied on startup", enterprise);
        assertEquals(Boolean.FALSE, enterprise.getAutoUpdateTools());
    }

    @Test
    public void persistedCache_appliedOnceOnly() {
        var json = "{\"appMap.autoUpdateTools\": false}";
        EnterpriseConfigCacheService.getInstance().setCacheJson(buildCacheJson("appmap:local-file", json));

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
        EnterpriseConfigCacheService.getInstance().setCacheJson(
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

    private static final String SPLUNK_JSON = """
            {
              "appMap.telemetry": {
                "backend": "splunk",
                "url": "https://splunk.example.com:443",
                "token": "tok",
                "ca": "system"
              }
            }
            """;

    private AtomicInteger subscribeTelemetryChanges() {
        var counter = new AtomicInteger();
        ApplicationManager.getApplication().getMessageBus().connect(getTestRootDisposable())
                .subscribe(AppMapSettingsListener.TOPIC, new AppMapSettingsListener() {
                    @Override
                    public void telemetrySettingsChanged() {
                        counter.incrementAndGet();
                    }
                });
        return counter;
    }

    private AtomicInteger subscribeDeploymentChanges() {
        var counter = new AtomicInteger();
        ApplicationManager.getApplication().getMessageBus().connect(getTestRootDisposable())
                .subscribe(AppMapSettingsListener.TOPIC, new AppMapSettingsListener() {
                    @Override
                    public void enterpriseDeploymentSettingsChanged() {
                        counter.incrementAndGet();
                    }
                });
        return counter;
    }

    /**
     * Runs {@code task} on a pooled thread and waits for it to finish. Used for operations that
     * publish settings-change events synchronously, so message-bus listeners run off the EDT and the
     * counters they update are fully visible once the task completes.
     */
    private void runOnPooledThreadAndWait(@NotNull Runnable task) throws InterruptedException {
        var latch = new CountDownLatch(1);
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                task.run();
            } finally {
                latch.countDown();
            }
        });
        assertTrue("Task did not complete within timeout", latch.await(5, TimeUnit.SECONDS));
    }

    private void waitUntil(@NotNull java.util.function.BooleanSupplier condition) throws InterruptedException {
        var deadline = System.currentTimeMillis() + 5_000;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) {
                fail("Condition not met within timeout");
            }
            //noinspection BusyWait
            Thread.sleep(25);
        }
    }

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
