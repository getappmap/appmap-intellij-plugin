package appland.enterpriseConfig;

import appland.deployment.AppMapDeploymentSettings;
import appland.deployment.AppMapDeploymentSettingsService;
import appland.deployment.AppMapDeploymentTelemetrySettings;
import appland.deployment.Entitlement;
import appland.notifications.AppMapNotifications;
import appland.settings.AppMapApplicationSettings;
import appland.settings.AppMapApplicationSettingsService;
import appland.settings.AppMapSettingsListener;
import appland.telemetry.TelemetryService;
import appland.utils.GsonUtils;
import com.google.gson.JsonObject;
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
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Service(Service.Level.APP)
public final class EnterpriseConfigService {
    private static final Logger LOG = Logger.getInstance(EnterpriseConfigService.class);
    private static final String LOCAL_FILE_SENTINEL = "appmap:local-file";

    private final AtomicBoolean cacheApplied = new AtomicBoolean(false);

    // Incremented by every apply/clear request. A fetch captures the value at the start and only
    // mutates state if it's still current, so a slow in-flight fetch can't clobber a newer Clear or
    // local-file apply. All state mutations happen under applyLock so check-and-apply is atomic.
    private final AtomicLong applyGeneration = new AtomicLong(0);
    private final Object applyLock = new Object();

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

    /**
     * Eagerly loads the configured organization config. Called once at startup from a background
     * thread. It applies the persisted cache (safe on any thread) and then, when a URL is configured,
     * performs the live fetch synchronously on the calling (background) thread.
     */
    public static void awaitInitialFetchIfConfigured() {
        var service = getInstance();
        var url = service.resolveConfigUrl();

        // Apply persisted cache synchronously — safe from any thread, just deserializes local JSON.
        // Runs even if url is null so one-shot (local-file) cache is restored on startup.
        service.applyPersistedCache(url);

        if (url == null) return;

        // This is meant to run on a background thread. If it's ever called on the EDT, don't block the
        // UI or run a network fetch on it — the cached/bundled settings are already in effect.
        if (ApplicationManager.getApplication().isDispatchThread()) {
            LOG.warn("Enterprise config fetch requested on the EDT; using cached/bundled settings instead of fetching to avoid blocking the UI.");
            return;
        }

        service.fetchAndApply(false, service.applyGeneration.incrementAndGet());
    }

    private void applyPersistedCache(@Nullable String currentUrl) {
        if (!cacheApplied.compareAndSet(false, true)) return;

        var cachedJson = EnterpriseConfigCacheService.getInstance().getCacheJson();
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

    /**
     * Fetches the organization config from the configured URL and applies it.
     *
     * @param interactive whether this was triggered by an explicit user action (vs. a startup fetch);
     *                    interactive failures clear stale config and notify the user.
     * @param gen         the apply-generation captured when this operation was requested; if a newer
     *                    apply/clear happens while we fetch, we abandon our result instead of clobbering it.
     */
    private void fetchAndApply(boolean interactive, long gen) {
        var deploymentService = AppMapDeploymentSettingsService.getInstance();
        var cacheService = EnterpriseConfigCacheService.getInstance();

        var url = resolveConfigUrl();
        if (url == null) {
            // URL was cleared at runtime. Preserve local-file one-shot settings if present,
            // since applyLocalFile() clears the URL as part of switching to local-file mode.
            // "before" stays null when nothing was mutated, so we don't fire listeners.
            EffectiveState before = null;
            EffectiveState after = null;
            synchronized (applyLock) {
                if (gen == applyGeneration.get() && !hasLocalFileCache(cacheService)) {
                    before = EffectiveState.capture();
                    deploymentService.setEnterpriseDeploymentSettings(null);
                    cacheService.setCacheJson(null);
                    after = EffectiveState.capture();
                }
            }
            // Fire outside the lock: listeners dispatch synchronously and must not run while we hold it.
            if (before != null) fireSettingsChanged(before, after);
            return;
        }

        // Perform the (potentially slow) fetch outside the lock.
        AppMapDeploymentSettings parsed;
        String content;
        try {
            if (url.startsWith("file:")) {
                content = Files.readString(fileUrlToPath(url));
            } else {
                content = HttpRequests.request(url).readString(null);
            }
            parsed = GsonUtils.GSON.fromJson(content, AppMapDeploymentSettings.class);
            if (parsed == null) {
                throw new JsonParseException("Organization configuration is not a valid JSON object");
            }
        } catch (IOException | JsonParseException | IllegalArgumentException e) {
            // IllegalArgumentException (incl. InvalidPathException) covers an unparseable file URL/path.
            LOG.warn("Enterprise config fetch failed for URL: " + maskCredentials(url), e);
            // On startup (non-interactive), keep whatever the persisted cache restored as an offline
            // fallback rather than wiping a known-good configuration on a transient failure.
            if (!interactive) return;

            // The user explicitly (re)applied this URL and it is unreachable or invalid: clear any
            // previously-applied org config so stale settings don't silently linger, and notify them.
            EffectiveState before = null;
            EffectiveState after = null;
            synchronized (applyLock) {
                if (gen == applyGeneration.get()) {
                    before = EffectiveState.capture();
                    deploymentService.setEnterpriseDeploymentSettings(null);
                    cacheService.setCacheJson(null);
                    after = EffectiveState.capture();
                }
            }
            if (before != null) {
                fireSettingsChanged(before, after); // outside the lock — see above
                notifyFetchFailed(url);
            }
            return;
        }

        // Apply (fast) atomically; skip if a newer apply/clear superseded this fetch.
        EffectiveState before = null;
        EffectiveState after = null;
        synchronized (applyLock) {
            if (gen == applyGeneration.get()) {
                before = EffectiveState.capture();
                // Capture the previous org config (same URL only) before overwriting the cache,
                // so we can tell which settings actually changed since the last fetch.
                var previousConfig = readCachedConfigForUrl(url);
                deploymentService.setEnterpriseDeploymentSettings(parsed);
                cacheService.setCacheJson(GsonUtils.GSON.toJson(new EnterpriseConfigCache(url, content)));
                markApplied();
                // Same URL → only supersede user overrides for settings that changed since last fetch;
                // a brand-new URL (no matching previous config) supersedes every setting it specifies.
                clearSupersededUserOverrides(AppMapApplicationSettingsService.getInstance(), parsed, previousConfig);
                LOG.debug("Applied enterprise settings from URL: " + maskCredentials(url));
                // Captured after clearSupersededUserOverrides so a superseded user override is reflected.
                after = EffectiveState.capture();
            }
        }
        // Fire outside the lock — listeners dispatch synchronously and must not run while we hold it.
        if (before != null) {
            fireSettingsChanged(before, after);
            if (interactive) notifyApplied();
        }
    }

    private boolean hasLocalFileCache(@NotNull EnterpriseConfigCacheService cacheService) {
        var cachedJson = cacheService.getCacheJson();
        if (cachedJson == null) return false;
        try {
            var cached = GsonUtils.GSON.fromJson(cachedJson, EnterpriseConfigCache.class);
            return cached != null && LOCAL_FILE_SENTINEL.equals(cached.url);
        } catch (JsonParseException e) {
            return false;
        }
    }

    private void notifyFetchFailed(@NotNull String url) {
        var projects = ProjectManager.getInstance().getOpenProjects();
        if (projects.length == 0) return;

        AppMapNotifications.showSimpleNotification(
                projects[0],
                "AppMap Organization Configuration",
                "Couldn't load the organization configuration from " + maskCredentials(url) +
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
    private @Nullable AppMapDeploymentSettings readCachedConfigForUrl(@NotNull String url) {
        var cachedJson = EnterpriseConfigCacheService.getInstance().getCacheJson();
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
        // Non-notifying setters: the caller fires a single enterpriseDeploymentSettingsChanged afterwards
        // (outside the apply lock), which the download/settings-panel listeners already react to. This
        // keeps message-bus dispatch out from under applyLock.
        maybeClearOverride("appMap.autoUpdateTools",
                newConfig.getAutoUpdateTools(),
                previousConfig != null ? previousConfig.getAutoUpdateTools() : null,
                previousConfig != null,
                settings.getAutoUpdateTools(),
                () -> settings.setAutoUpdateTools(null));

        maybeClearOverride("appMap.manifest.appmapUrl",
                newConfig.getAppmapManifestUrl(),
                previousConfig != null ? previousConfig.getAppmapManifestUrl() : null,
                previousConfig != null,
                settings.getAppmapManifestUrl(),
                () -> settings.setAppmapManifestUrl(null));

        maybeClearOverride("appMap.manifest.scannerUrl",
                newConfig.getScannerManifestUrl(),
                previousConfig != null ? previousConfig.getScannerManifestUrl() : null,
                previousConfig != null,
                settings.getScannerManifestUrl(),
                () -> settings.setScannerManifestUrl(null));

        maybeClearOverride("appMap.scannerEnabled",
                newConfig.getScannerEnabled(),
                previousConfig != null ? previousConfig.getScannerEnabled() : null,
                previousConfig != null,
                settings.getEnableScanner(),
                () -> settings.setEnableScanner(null));
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

    /**
     * Announces an organization-config mutation, deriving what actually needs to react from the effective
     * state captured either side of it. Every mutation site funnels through here, so this is the single place
     * deciding whether to restart services or rebuild the telemetry reporter.
     */
    private void fireSettingsChanged(@NotNull EffectiveState before, @NotNull EffectiveState after) {
        var publisher = ApplicationManager.getApplication().getMessageBus()
                .syncPublisher(AppMapSettingsListener.TOPIC);
        publisher.enterpriseDeploymentSettingsChanged();

        // Only fire when the effective scanner state actually changed: the listener restarts the
        // indexer/scanner CLI processes and rebuilds the tool window, so it must not run on every
        // org-config apply (e.g. a manifest-only change).
        if (before.scannerEnabled() != after.scannerEnabled()) {
            publisher.scannerEnabledChanged();
        }

        var telemetryChanged = !Objects.equals(before.telemetry(), after.telemetry());
        var customerIdChanged = !Objects.equals(before.customerId(), after.customerId());

        // The reporter is built with a fixed set of common properties, which includes the customer ID, so
        // either change means rebuilding it — otherwise events keep the stale attribution or the stale
        // backend until the IDE restarts. Only act if the telemetry service already exists; otherwise it
        // picks up the current settings when it is created.
        if (telemetryChanged || customerIdChanged) {
            var telemetryService = ApplicationManager.getApplication().getServiceIfCreated(TelemetryService.class);
            if (telemetryService != null) {
                telemetryService.reloadReporter();
            }
        }

        // Routing is a separate, more expensive concern: this event also restarts the processes which carry
        // telemetry settings in their environment, so it is not fired for a customer-ID-only change.
        if (telemetryChanged) {
            publisher.telemetrySettingsChanged();
        }

        // Entitlement makes the plugin behave as signed in, so a change here starts or stops the services and
        // swaps the tool window, exactly as signing in or out does.
        if (customerIdChanged) {
            publisher.customerIdChanged();
        }
    }

    /**
     * Snapshot of everything whose <em>effective</em> value an organization-config mutation can move and which
     * some listener has to react to. Captured before and after each mutation, so
     * {@link #fireSettingsChanged} derives what changed instead of every call site hand-rolling a
     * before/after pair per field.
     * <p>
     * "Effective" means resolved across all the layers that contribute: a user override still wins over the
     * organization config for the scanner, which is why capturing {@code after} has to happen once every
     * mutation — including {@link #clearSupersededUserOverrides} — is done.
     */
    private record EffectiveState(@Nullable AppMapDeploymentTelemetrySettings telemetry,
                                  boolean scannerEnabled,
                                  @Nullable String customerId) {
        static @NotNull EffectiveState capture() {
            return new EffectiveState(
                    AppMapDeploymentSettingsService.getCachedDeploymentSettings().getTelemetry(),
                    AppMapApplicationSettingsService.getInstance().isScannerEnabled(),
                    Entitlement.getCustomerId());
        }
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
        var gen = applyGeneration.incrementAndGet();
        ApplicationManager.getApplication().executeOnPooledThread(() -> fetchAndApply(true, gen));
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
        // A local-file snapshot is the active source for this session even if APPMAP_CONFIG_URL is set
        // (the env URL would only be (re)applied on the next startup), so check it first.
        if (hasLocalFileCache(EnterpriseConfigCacheService.getInstance())) {
            return "local file";
        }
        var settings = AppMapApplicationSettingsService.getInstance();
        var url = settings.getConfigurationUrl();
        if (StringUtil.isNotEmpty(url)) {
            return "URL: " + maskCredentials(url);
        }
        var envUrl = System.getenv("APPMAP_CONFIG_URL");
        if (StringUtil.isNotEmpty(envUrl)) {
            return "URL: " + maskCredentials(envUrl) + " (from APPMAP_CONFIG_URL)";
        }
        if (settings.getOrgConfigAppliedAt() != null) {
            return "local file";
        }
        return null;
    }

    /**
     * Strips userinfo (user:password@) from a URL so credentials don't leak into logs, the status
     * report, or notifications. URLs without credentials are returned unchanged.
     */
    private static @NotNull String maskCredentials(@NotNull String url) {
        return url.replaceFirst("://[^@/]*@", "://***@");
    }

    /**
     * Converts a {@code file:} URL to a path. Prefers proper file-URI parsing (handles {@code file:///},
     * escaped characters, UNC hosts), but falls back to treating the part after {@code file://} as a raw
     * OS path — which is how the URL is often built (e.g. {@code "file://" + path}, yielding
     * {@code file://C:\...} on Windows, which is not a valid URI).
     */
    private static @NotNull Path fileUrlToPath(@NotNull String url) {
        try {
            return Path.of(URI.create(url));
        } catch (IllegalArgumentException e) {
            var prefix = url.startsWith("file://") ? "file://" : "file:";
            return Path.of(url.substring(prefix.length()));
        }
    }

    /**
     * @return The JSON contents of the currently-applied organization configuration (as fetched or read
     * from file) with the Splunk telemetry {@code token} redacted, or {@code null} if none is cached.
     * Intended for the plugin status / debugging view, which users routinely paste into bug reports — so
     * the secret token must never appear here. (The {@code ca} certificate is public and is left intact.)
     */
    public @Nullable String getAppliedConfigJson() {
        var cachedJson = EnterpriseConfigCacheService.getInstance().getCacheJson();
        if (cachedJson == null) return null;
        try {
            var cached = GsonUtils.GSON.fromJson(cachedJson, EnterpriseConfigCache.class);
            if (cached == null || cached.json == null) return null;
            return redactSecrets(cached.json);
        } catch (JsonParseException e) {
            return null;
        }
    }

    private static @NotNull String redactSecrets(@NotNull String configJson) {
        try {
            var root = GsonUtils.GSON.fromJson(configJson, JsonObject.class);
            if (root != null && root.has("appMap.telemetry") && root.get("appMap.telemetry").isJsonObject()) {
                var telemetry = root.getAsJsonObject("appMap.telemetry");
                if (telemetry.has("token")) {
                    telemetry.addProperty("token", "***");
                }
            }
            return GsonUtils.GSON.toJson(root);
        } catch (Exception e) {
            // Never fall back to the raw JSON — that could leak the token. Return a safe placeholder.
            return "{ \"error\": \"organization configuration present but could not be safely rendered\" }";
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

        // Bump the generation so any in-flight fetch is superseded and won't re-apply after we clear.
        applyGeneration.incrementAndGet();
        final EffectiveState before;
        final EffectiveState after;
        synchronized (applyLock) {
            before = EffectiveState.capture();
            deploymentService.setEnterpriseDeploymentSettings(null);
            EnterpriseConfigCacheService.getInstance().setCacheJson(null);
            applicationSettings.setOrgConfigAppliedAt(null);
            // Clear the URL non-notifying: we clear the state directly here, so a configurationUrlChanged
            // would only spawn a redundant apply pipeline (and a duplicate telemetry reload).
            applicationSettings.setConfigurationUrl(null);
            after = EffectiveState.capture();
        }
        // Fire outside the lock — listeners dispatch synchronously and must not run while we hold it.
        fireSettingsChanged(before, after);

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

        // Bump the generation so any in-flight URL fetch is superseded and won't clobber this apply.
        applyGeneration.incrementAndGet();
        final EffectiveState before;
        final EffectiveState after;
        synchronized (applyLock) {
            before = EffectiveState.capture();

            // Clear any existing URL-based config. Clear the URL non-notifying: a configurationUrlChanged
            // here would kick off a second, redundant apply pipeline. We apply the file directly.
            if (resolveConfigUrl() != null) {
                deploymentService.setEnterpriseDeploymentSettings(null);
                applicationSettings.setConfigurationUrl(null);
            }

            deploymentService.setEnterpriseDeploymentSettings(parsed);
            EnterpriseConfigCacheService.getInstance().setCacheJson(
                    GsonUtils.GSON.toJson(new EnterpriseConfigCache(LOCAL_FILE_SENTINEL, fileContent)));
            markApplied();
            // A local-file apply is a one-shot, fresh source: supersede every user override it specifies.
            clearSupersededUserOverrides(applicationSettings, parsed, null);
            // Captured after clearSupersededUserOverrides so a superseded user override is reflected.
            after = EffectiveState.capture();
        }

        notifyApplied();
        ApplicationManager.getApplication().executeOnPooledThread(() -> fireSettingsChanged(before, after));
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
        cacheApplied.set(false);
        applyGeneration.set(0);
        EnterpriseConfigCacheService.getInstance().setCacheJson(null);
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
