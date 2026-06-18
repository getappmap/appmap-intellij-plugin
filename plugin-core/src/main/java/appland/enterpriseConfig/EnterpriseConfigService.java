package appland.enterpriseConfig;

import appland.deployment.AppMapDeploymentSettings;
import appland.deployment.AppMapDeploymentSettingsService;
import appland.settings.AppMapApplicationSettingsService;
import appland.settings.AppMapSettingsListener;
import appland.telemetry.TelemetryService;
import appland.utils.GsonUtils;
import com.google.gson.JsonParseException;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.io.HttpRequests;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
            ApplicationManager.getApplication().executeOnPooledThread(this::fetchAndApply);
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

    private void fetchAndApply() {
        var deploymentService = AppMapDeploymentSettingsService.getInstance();
        var applicationSettings = AppMapApplicationSettingsService.getInstance();
        var currentFuture = fetchFutureRef.get();

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
            fireSettingsChanged();
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
                if (parsed != null) {
                    deploymentService.setEnterpriseDeploymentSettings(parsed);
                    var cache = new EnterpriseConfigCache(url, content);
                    applicationSettings.setEnterpriseConfigCache(GsonUtils.GSON.toJson(cache));
                    markApplied();
                    LOG.debug("Applied enterprise settings from URL: " + url);
                }
            } catch (IOException | JsonParseException e) {
                LOG.warn("Enterprise config fetch failed for URL: " + url, e);
            }
        } finally {
            if (currentFuture != null) currentFuture.complete(null);
            IS_FETCHING.set(false);
        }

        fireSettingsChanged();
    }

    private void fireSettingsChanged() {
        ApplicationManager.getApplication().getMessageBus()
                .syncPublisher(AppMapSettingsListener.TOPIC)
                .enterpriseDeploymentSettingsChanged();

        // Reconfigure telemetry routing on the fly so no IDE restart is required.
        // Only act if the telemetry service has already been initialized; otherwise it will
        // pick up the current settings when it's first created.
        var telemetryService = ApplicationManager.getApplication().getServiceIfCreated(TelemetryService.class);
        if (telemetryService != null) {
            telemetryService.reloadReporter();
        }
    }

    public void applyAsync() {
        fetchFutureRef.set(null); // allow re-fetch
        ApplicationManager.getApplication().executeOnPooledThread(this::fetchAndApply);
    }

    public void markApplied() {
        AppMapApplicationSettingsService.getInstance().setOrgConfigAppliedAt(System.currentTimeMillis());
    }

    public boolean isApplied() {
        return resolveConfigUrl() != null ||
                AppMapApplicationSettingsService.getInstance().getOrgConfigAppliedAt() != null;
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

        // Clear any existing URL-based config
        var existingUrl = resolveConfigUrl();
        if (existingUrl != null) {
            deploymentService.setEnterpriseDeploymentSettings(null);
            applicationSettings.setEnterpriseConfigCache(null);
            var urlFromSettings = applicationSettings.getConfigurationUrl();
            if (StringUtil.isNotEmpty(urlFromSettings)) {
                applicationSettings.setConfigurationUrlNotifying(null);
            }
        }

        deploymentService.setEnterpriseDeploymentSettings(parsed);
        var cache = new EnterpriseConfigCache(LOCAL_FILE_SENTINEL, fileContent);
        applicationSettings.setEnterpriseConfigCache(GsonUtils.GSON.toJson(cache));
        markApplied();

        ApplicationManager.getApplication().executeOnPooledThread(this::fireSettingsChanged);
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
