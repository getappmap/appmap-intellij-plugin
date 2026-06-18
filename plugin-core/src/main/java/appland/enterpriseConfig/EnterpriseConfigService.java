package appland.enterpriseConfig;

import appland.deployment.AppMapDeploymentSettings;
import appland.deployment.AppMapDeploymentSettingsService;
import appland.deployment.AppMapDeploymentTelemetrySettings;
import appland.notifications.AppMapNotifications;
import appland.settings.AppMapApplicationSettings;
import appland.settings.AppMapApplicationSettingsService;
import appland.settings.AppMapSettingsListener;
import appland.telemetry.TelemetryService;
import appland.utils.GsonUtils;
import com.google.gson.JsonParseException;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.io.HttpRequests;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Service(Service.Level.APP)
public final class EnterpriseConfigService {
    private static final Logger LOG = Logger.getInstance(EnterpriseConfigService.class);
    private static final String LOCAL_FILE_SENTINEL = "appmap:local-file";
    private static final ThreadLocal<Boolean> IS_FETCHING = ThreadLocal.withInitial(() -> false);

    private final AtomicReference<CompletableFuture<Void>> fetchFutureRef = new AtomicReference<>(null);
    private final AtomicBoolean cacheApplied = new AtomicBoolean(false);

    public static @NotNull EnterpriseConfigService getInstance() {
        return ApplicationManager.getApplication().getService(EnterpriseConfigService.class);
    }

    public @Nullable String resolveConfigUrl() {
        var fromSettings = AppMapApplicationSettingsService.getInstance().getConfigurationUrl();
        if (StringUtil.isNotEmpty(fromSettings)) return fromSettings;
        return System.getenv("APPMAP_CONFIG_URL");
    }

    /**
     * Applies the persisted organization-config cache if it hasn't been applied yet. Safe to call from
     * any thread (including the EDT): it only deserializes local JSON and never triggers or waits on a
     * network fetch. This is the read path used by {@code getDeploymentSettings()}; the live fetch is
     * kicked off eagerly at startup (and on URL changes) instead of lazily on first read.
     */
    public void ensurePersistedCacheApplied() {
        applyPersistedCache(resolveConfigUrl());
    }

    public static void awaitInitialFetchIfConfigured() {
        var service = getInstance();
        var url = service.resolveConfigUrl();

        // Apply persisted cache synchronously — safe from any thread, just deserializes local JSON.
        // Runs even if url is null so one-shot (local-file) cache is restored on startup.
        service.applyPersistedCache(url);

        if (url == null) return;

        // Only block waiting for the live fetch if we're on a background thread and not re-entrant.
        if (ApplicationManager.getApplication().isDispatchThread()) {
            LOG.error("Enterprise config wait requested on EDT with an active config URL. This is a logic error; the UI will use cached/bundled settings instead of waiting for organization config to prevent hangs.");
            return;
        }
        if (IS_FETCHING.get()) {
            LOG.error("Re-entrant call to getDeploymentSettings detected on fetch thread. This is a logic error: settings should not be read while they are being updated mid-fetch. Returning current/stale settings to avoid deadlock.");
            return;
        }

        var future = service.getOrStartFetchFuture();
        try {
            future.get(3_500, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            LOG.warn("Enterprise config fetch timed out after 3.5s, using cached settings if available");
        } catch (ExecutionException | InterruptedException e) {
            LOG.warn("Enterprise config fetch failed", e);
        }
    }

    private @NotNull CompletableFuture<Void> getOrStartFetchFuture() {
        var existing = fetchFutureRef.get();
        if (existing != null) return existing;

        var newFuture = new CompletableFuture<Void>();
        if (fetchFutureRef.compareAndSet(null, newFuture)) {
            // Lazy/startup fetch: not interactive, so a failure keeps the cached offline fallback.
            ApplicationManager.getApplication().executeOnPooledThread(() -> fetchAndApply(false));
            return newFuture;
        }
        // Another thread beat us, return the winner
        return fetchFutureRef.get();
    }

    private void applyPersistedCache(@Nullable String currentUrl) {
        if (!cacheApplied.compareAndSet(false, true)) return;

        var applicationSettings = AppMapApplicationSettingsService.getInstance();
        var cachedJson = applicationSettings.getEnterpriseConfigCache();
        if (cachedJson == null) return;

        try {
            var cached = GsonUtils.GSON.fromJson(cachedJson, EnterpriseConfigCache.class);
            if (cached == null) return;

            boolean shouldApply = LOCAL_FILE_SENTINEL.equals(cached.url) ||
                    (currentUrl != null && currentUrl.equals(cached.url));

            if (shouldApply && cached.json != null) {
                var parsed = GsonUtils.GSON.fromJson(cached.json, AppMapDeploymentSettings.class);
                if (parsed != null) {
                    AppMapDeploymentSettingsService.getInstance().setEnterpriseDeploymentSettings(parsed);
                    LOG.debug("Applied enterprise settings from cache (url=" + cached.url + ")");
                }
            }
        } catch (JsonParseException e) {
            LOG.warn("Failed to deserialize enterprise config cache", e);
        }
    }

    private void fetchAndApply(boolean interactive) {
        var deploymentService = AppMapDeploymentSettingsService.getInstance();
        var applicationSettings = AppMapApplicationSettingsService.getInstance();
        var currentFuture = fetchFutureRef.get();

        var telemetryBefore = effectiveTelemetry();

        var url = resolveConfigUrl();
        if (url == null) {
            // URL was cleared at runtime. Preserve local-file one-shot settings if present,
            // since applyLocalFile() clears the URL as part of switching to local-file mode.
            var cachedJson = applicationSettings.getEnterpriseConfigCache();
            boolean hasLocalFileSentinel = false;
            if (cachedJson != null) {
                try {
                    var cached = GsonUtils.GSON.fromJson(cachedJson, EnterpriseConfigCache.class);
                    hasLocalFileSentinel = cached != null && LOCAL_FILE_SENTINEL.equals(cached.url);
                } catch (JsonParseException ignored) {}
            }
            if (!hasLocalFileSentinel) {
                deploymentService.setEnterpriseDeploymentSettings(null);
                applicationSettings.setEnterpriseConfigCache(null);
            }
            if (currentFuture != null) currentFuture.complete(null);
            fireSettingsChanged(telemetryChanged(telemetryBefore));
            return;
        }

        IS_FETCHING.set(true);
        try {
            try {
                String content;
                if (url.startsWith("file://")) {
                    content = Files.readString(Path.of(url.substring("file://".length())));
                } else {
                    content = HttpRequests.request(url).readString(null);
                }
                var parsed = GsonUtils.GSON.fromJson(content, AppMapDeploymentSettings.class);
                if (parsed == null) {
                    throw new JsonParseException("Organization configuration is not a valid JSON object");
                }
                // Capture the previous org config (same URL only) before overwriting the cache,
                // so we can tell which settings actually changed since the last fetch.
                var previousConfig = readCachedConfigForUrl(applicationSettings, url);
                deploymentService.setEnterpriseDeploymentSettings(parsed);
                var cache = new EnterpriseConfigCache(url, content);
                applicationSettings.setEnterpriseConfigCache(GsonUtils.GSON.toJson(cache));
                markApplied();
                // Same URL → only supersede user overrides for settings that changed since last fetch;
                // a brand-new URL (no matching previous config) supersedes every setting it specifies.
                clearSupersededUserOverrides(applicationSettings, parsed, previousConfig);
                LOG.debug("Applied enterprise settings from URL: " + url);
                if (interactive) {
                    notifyApplied();
                }
            } catch (IOException | JsonParseException e) {
                LOG.warn("Enterprise config fetch failed for URL: " + url, e);
                if (interactive) {
                    // The user explicitly (re)applied this URL and it is unreachable or invalid.
                    // Clear any previously-applied org config so stale settings don't silently linger,
                    // and let the user know their organization configuration is not being applied.
                    deploymentService.setEnterpriseDeploymentSettings(null);
                    applicationSettings.setEnterpriseConfigCache(null);
                    notifyFetchFailed(url);
                }
                // On startup (non-interactive), keep whatever the persisted cache restored as an
                // offline fallback rather than wiping a known-good configuration on a transient failure.
            }
        } finally {
            if (currentFuture != null) currentFuture.complete(null);
            IS_FETCHING.set(false);
        }

        fireSettingsChanged(telemetryChanged(telemetryBefore));
    }

    private void notifyFetchFailed(@NotNull String url) {
        var projects = ProjectManager.getInstance().getOpenProjects();
        if (projects.length == 0) return;

        AppMapNotifications.showSimpleNotification(
                projects[0],
                "AppMap Organization Configuration",
                "Couldn't load the organization configuration from " + url +
                        ". Any previously applied organization settings have been cleared.",
                NotificationType.WARNING,
                true
        );
    }

    /**
     * Parses the cached organization config for {@code url}, or {@code null} if the cache is empty,
     * unparseable, or was stored for a different URL/source. A non-null result means "same source as
     * last time", which lets {@link #clearSupersededUserOverrides} restrict itself to changed settings.
     */
    private @Nullable AppMapDeploymentSettings readCachedConfigForUrl(
            @NotNull AppMapApplicationSettings applicationSettings, @NotNull String url) {
        var cachedJson = applicationSettings.getEnterpriseConfigCache();
        if (cachedJson == null) return null;
        try {
            var cached = GsonUtils.GSON.fromJson(cachedJson, EnterpriseConfigCache.class);
            if (cached == null || cached.json == null || !url.equals(cached.url)) return null;
            return GsonUtils.GSON.fromJson(cached.json, AppMapDeploymentSettings.class);
        } catch (JsonParseException e) {
            return null;
        }
    }

    /**
     * Removes user overrides that the freshly-applied organization config should win over, so applying
     * an org config actually takes effect instead of being silently masked by an existing user setting.
     *
     * <p>If {@code previousConfig} is {@code null} (a new URL, a local file, or a first-time fetch) every
     * setting the new config specifies supersedes the matching user override. If it is non-null (a repeat
     * fetch from the same URL) only settings whose org value changed since last time are superseded, so a
     * user can re-override a setting the org config isn't actively changing. The user can always override
     * again afterwards.
     */
    private void clearSupersededUserOverrides(@NotNull AppMapApplicationSettings settings,
                                              @NotNull AppMapDeploymentSettings newConfig,
                                              @Nullable AppMapDeploymentSettings previousConfig) {
        maybeClearOverride("appMap.autoUpdateTools",
                newConfig.getAutoUpdateTools(),
                previousConfig != null ? previousConfig.getAutoUpdateTools() : null,
                previousConfig != null,
                settings.getAutoUpdateTools(),
                () -> settings.setAutoUpdateToolsNotifying(null));

        maybeClearOverride("appMap.manifest.appmapUrl",
                newConfig.getAppmapManifestUrl(),
                previousConfig != null ? previousConfig.getAppmapManifestUrl() : null,
                previousConfig != null,
                settings.getAppmapManifestUrl(),
                () -> settings.setAppmapManifestUrlNotifying(null));

        maybeClearOverride("appMap.manifest.scannerUrl",
                newConfig.getScannerManifestUrl(),
                previousConfig != null ? previousConfig.getScannerManifestUrl() : null,
                previousConfig != null,
                settings.getScannerManifestUrl(),
                () -> settings.setScannerManifestUrlNotifying(null));
    }

    private void maybeClearOverride(@NotNull String key,
                                    @Nullable Object newOrgValue,
                                    @Nullable Object previousOrgValue,
                                    boolean sameSource,
                                    @Nullable Object currentUserOverride,
                                    @NotNull Runnable clearOverride) {
        if (newOrgValue == null) return;            // org config has no opinion on this setting
        if (currentUserOverride == null) return;    // no user override to supersede
        // Same source: only supersede when the org value actually changed since the last fetch.
        if (sameSource && Objects.equals(newOrgValue, previousOrgValue)) return;

        clearOverride.run();
        LOG.info("Organization configuration superseded user setting '" + key + "' (was "
                + currentUserOverride + ", now using organization value " + newOrgValue + ")");
    }

    private void fireSettingsChanged(boolean telemetryChanged) {
        var publisher = ApplicationManager.getApplication().getMessageBus()
                .syncPublisher(AppMapSettingsListener.TOPIC);
        publisher.enterpriseDeploymentSettingsChanged();

        // Telemetry routing is comparatively expensive to reconfigure (it rebuilds the reporter and
        // restarts processes that carry telemetry settings in their environment), so only do it when
        // the telemetry settings actually changed.
        if (telemetryChanged) {
            // Reconfigure telemetry routing on the fly so no IDE restart is required. Only act if the
            // telemetry service already exists; otherwise it picks up the current settings when created.
            var telemetryService = ApplicationManager.getApplication().getServiceIfCreated(TelemetryService.class);
            if (telemetryService != null) {
                telemetryService.reloadReporter();
            }
            publisher.telemetrySettingsChanged();
        }
    }

    /**
     * @return The effective telemetry settings (organization config merged over bundled defaults),
     * used to decide whether telemetry-dependent components need to be reconfigured/restarted.
     */
    private @Nullable AppMapDeploymentTelemetrySettings effectiveTelemetry() {
        return AppMapDeploymentSettingsService.getCachedDeploymentSettings().getTelemetry();
    }

    private boolean telemetryChanged(@Nullable AppMapDeploymentTelemetrySettings before) {
        return !Objects.equals(before, effectiveTelemetry());
    }

    private void notifyApplied() {
        var projects = ProjectManager.getInstance().getOpenProjects();
        if (projects.length == 0) return;

        var source = getConfigSourceDescription();
        AppMapNotifications.showSimpleNotification(
                projects[0],
                "AppMap Organization Configuration",
                source != null
                        ? "Organization configuration applied (" + source + ")."
                        : "Organization configuration applied.",
                NotificationType.INFORMATION,
                true
        );
    }

    /**
     * Re-applies the configured organization config from its URL. Used when the user changes the
     * configuration URL interactively, so a fetch failure clears any previously-applied org config
     * and notifies the user instead of silently keeping stale settings.
     */
    public void applyAsync() {
        fetchFutureRef.set(null); // allow re-fetch
        ApplicationManager.getApplication().executeOnPooledThread(() -> fetchAndApply(true));
    }

    public void markApplied() {
        AppMapApplicationSettingsService.getInstance().setOrgConfigAppliedAt(System.currentTimeMillis());
    }

    public boolean isApplied() {
        return resolveConfigUrl() != null ||
                AppMapApplicationSettingsService.getInstance().getOrgConfigAppliedAt() != null;
    }

    /**
     * @return A human-readable description of where the currently-applied organization configuration
     * comes from, or {@code null} if none is applied.
     */
    public @Nullable String getConfigSourceDescription() {
        var settings = AppMapApplicationSettingsService.getInstance();
        var url = settings.getConfigurationUrl();
        if (StringUtil.isNotEmpty(url)) {
            return "URL: " + url;
        }
        var envUrl = System.getenv("APPMAP_CONFIG_URL");
        if (StringUtil.isNotEmpty(envUrl)) {
            return "URL: " + envUrl + " (from APPMAP_CONFIG_URL)";
        }
        if (settings.getOrgConfigAppliedAt() != null) {
            return "local file";
        }
        return null;
    }

    /**
     * @return The raw JSON contents of the currently-applied organization configuration (as fetched or
     * read from file), or {@code null} if none is cached. Intended for the plugin status / debugging view.
     */
    public @Nullable String getAppliedConfigJson() {
        var cachedJson = AppMapApplicationSettingsService.getInstance().getEnterpriseConfigCache();
        if (cachedJson == null) return null;
        try {
            var cached = GsonUtils.GSON.fromJson(cachedJson, EnterpriseConfigCache.class);
            return cached != null ? cached.json : null;
        } catch (JsonParseException e) {
            return null;
        }
    }

    /**
     * Removes any applied organization configuration: clears the configured URL, the cached config,
     * the in-memory enterprise settings and the applied-timestamp, then notifies listeners. User
     * settings are left untouched (they keep whatever values are currently effective).
     */
    public void clearOrgConfig() {
        var deploymentService = AppMapDeploymentSettingsService.getInstance();
        var applicationSettings = AppMapApplicationSettingsService.getInstance();

        var telemetryBefore = effectiveTelemetry();

        fetchFutureRef.set(null);
        deploymentService.setEnterpriseDeploymentSettings(null);
        applicationSettings.setEnterpriseConfigCache(null);
        applicationSettings.setOrgConfigAppliedAt(null);
        // Clear the URL non-notifying: we clear the state directly here, so a configurationUrlChanged
        // would only spawn a redundant apply pipeline (and a duplicate telemetry reload).
        applicationSettings.setConfigurationUrl(null);

        fireSettingsChanged(telemetryChanged(telemetryBefore));

        if (StringUtil.isNotEmpty(System.getenv("APPMAP_CONFIG_URL"))) {
            LOG.warn("Organization config is also set via the APPMAP_CONFIG_URL environment variable; " +
                    "it will be re-applied on the next startup unless the variable is unset.");
        }
    }

    public void applyLocalFile(@NotNull String fileContent, @Nullable com.intellij.openapi.project.Project project) {
        var deploymentService = AppMapDeploymentSettingsService.getInstance();
        var applicationSettings = AppMapApplicationSettingsService.getInstance();

        AppMapDeploymentSettings parsed;
        try {
            parsed = GsonUtils.GSON.fromJson(fileContent, AppMapDeploymentSettings.class);
            if (parsed == null) {
                showLocalFileError(project, "The configuration file is not a valid JSON object.");
                return;
            }
        } catch (JsonParseException e) {
            showLocalFileError(project, "Failed to parse configuration file: " + e.getMessage());
            return;
        }

        var telemetryBefore = effectiveTelemetry();

        // Clear any existing URL-based config
        var existingUrl = resolveConfigUrl();
        if (existingUrl != null) {
            fetchFutureRef.set(null); // abandon any URL-based fetch state; we're switching to a local file
            deploymentService.setEnterpriseDeploymentSettings(null);
            applicationSettings.setEnterpriseConfigCache(null);
            // Clear the URL non-notifying: a configurationUrlChanged here would kick off a second,
            // redundant apply pipeline (and a duplicate telemetry reload). We apply the file directly.
            applicationSettings.setConfigurationUrl(null);
        }

        deploymentService.setEnterpriseDeploymentSettings(parsed);
        var cache = new EnterpriseConfigCache(LOCAL_FILE_SENTINEL, fileContent);
        applicationSettings.setEnterpriseConfigCache(GsonUtils.GSON.toJson(cache));
        markApplied();
        // A local-file apply is a one-shot, fresh source: supersede every user override it specifies.
        clearSupersededUserOverrides(applicationSettings, parsed, null);

        notifyApplied();
        var telemetryChanged = telemetryChanged(telemetryBefore);
        ApplicationManager.getApplication().executeOnPooledThread(() -> fireSettingsChanged(telemetryChanged));
    }

    private void showLocalFileError(@Nullable com.intellij.openapi.project.Project project, @NotNull String message) {
        LOG.warn("Enterprise config error: " + message);
        if (!ApplicationManager.getApplication().isHeadlessEnvironment()) {
            ApplicationManager.getApplication().invokeLater(() ->
                    com.intellij.openapi.ui.Messages.showErrorDialog(project, message, "Organization Configuration Error"));
        }
    }

    @TestOnly
    public void reset() {
        fetchFutureRef.set(null);
        cacheApplied.set(false);
        IS_FETCHING.set(false);
    }

    private static class EnterpriseConfigCache {
        public String url;
        public String json;

        public EnterpriseConfigCache(String url, String json) {
            this.url = url;
            this.json = json;
        }
    }
}
