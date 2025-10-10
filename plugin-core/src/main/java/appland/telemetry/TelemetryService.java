package appland.telemetry;

import appland.AppMapBundle;
import appland.deployment.AppMapDeploymentSettings;
import appland.deployment.AppMapDeploymentSettingsService;
import appland.notifications.AppMapNotifications;
import appland.settings.AppMapApplicationSettingsService;
import appland.telemetry.appinsights.AppInsightsTelemetryReporter;
import appland.telemetry.splunk.SplunkTelemetryReporter;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class TelemetryService {
    private static final @NotNull Logger LOG = Logger.getInstance(TelemetryService.class);

    private final @NotNull TelemetryReporter reporter;

    public static @NotNull TelemetryService getInstance() {
        return ApplicationManager.getApplication().getService(TelemetryService.class);
    }

    public TelemetryService() {
        TelemetryReporter reporter;
        try {
            reporter = createTelemetryReporter(AppMapDeploymentSettingsService.getCachedDeploymentSettings());
        } catch (Exception e) {
            // safeguard to prevent unexpected exceptions breaking this service and the plugin
            LOG.debug("Failed to create telemetry reporter. Using no-op reporter.", e);
            reporter = new NoOpTelemetryReporter();
        }

        this.reporter = reporter;
    }

    /**
     * @return If telemetry is enabled, either with the user settings or forced by the configured telemetry reporter.
     */
    public boolean isEnabled() {
        return AppMapApplicationSettingsService.getInstance().isEnableTelemetry() || reporter.isAlwaysEnabled();
    }

    /**
     * @return The configured telemetry reporter, useful for status reporting.
     */
    public @NotNull TelemetryReporter getReporter() {
        return reporter;
    }

    public void notifyTelemetryUsage(@NotNull Project project) {
        AppMapNotifications.showTelemetryNotification(
                project,
                AppMapBundle.get("telemetry.permission.title"),
                AppMapBundle.get("telemetry.permission.message"),
                NotificationType.INFORMATION,
                (enabled) -> AppMapApplicationSettingsService.getInstance().setEnableTelemetry(enabled));
    }

    public void sendEvent(@NotNull String name) {
        sendEvent(new TelemetryEvent(name));
    }

    /**
     * Sends data to the telemetry server. The request is sent in a background thread.
     *
     * @param event Tracked event
     */
    public void sendEvent(@NotNull TelemetryEvent event) {
        if (!isEnabled()) {
            LOG.debug("Telemetry is disabled. Skipping event.");
            return;
        }

        ApplicationManager.getApplication().executeOnPooledThread(() -> reporter.track(event));
    }

    private static @NotNull TelemetryReporter createTelemetryReporter(@Nullable AppMapDeploymentSettings settings) {
        if (SplunkTelemetryUtils.isSplunkTelemetryEnabled(settings)) {
            LOG.debug("Splunk telemetry is enabled");

            var telemetry = Objects.requireNonNull(settings.getTelemetry());
            // If the `backend` is set to `splunk` but the `url` or `token` are missing, telemetry data will not be sent.
            if (SplunkTelemetryUtils.isSplunkTelemetryEnabled(settings)) {
                LOG.debug("Splunk telemetry is enabled and active. Creating Splunk telemetry reporter.");
                return new SplunkTelemetryReporter(
                        Objects.requireNonNull(telemetry.getUrl()),
                        Objects.requireNonNull(telemetry.getToken()));
            }

            LOG.debug("Splunk telemetry is enabled but inactive. Creating no-op telemetry reporter.");
            return new NoOpTelemetryReporter();
        }

        LOG.debug("Creating AppInsights telemetry reporter.");
        return new AppInsightsTelemetryReporter();
    }
}
