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
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class TelemetryService {
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
            reporter = new NoOpTelemetryReporter();
        }

        this.reporter = reporter;
    }

    public boolean isEnabled() {
        return AppMapApplicationSettingsService.getInstance().isEnableTelemetry();
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
            return;
        }

        ApplicationManager.getApplication().executeOnPooledThread(() -> reporter.track(event));
    }

    private static @NotNull TelemetryReporter createTelemetryReporter(@Nullable AppMapDeploymentSettings settings) {
        if (SplunkTelemetryUtils.isSplunkTelemetryEnabled(settings)) {
            var telemetry = Objects.requireNonNull(settings.getTelemetry());
            // If the `backend` is set to `splunk` but the `url` or `token` are missing, telemetry data will not be sent.
            if (StringUtil.isEmpty(telemetry.getToken()) || StringUtil.isEmpty(telemetry.getUrl())) {
                return new NoOpTelemetryReporter();
            }

            return new SplunkTelemetryReporter(telemetry.getUrl(), telemetry.getToken());
        }

        return new AppInsightsTelemetryReporter();
    }
}
