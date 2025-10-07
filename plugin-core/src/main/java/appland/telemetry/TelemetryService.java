package appland.telemetry;

import appland.AppMapBundle;
import appland.notifications.AppMapNotifications;
import appland.settings.AppMapApplicationSettingsService;
import appland.telemetry.appinsights.AppInsightsTelemetryReporter;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class TelemetryService {
    @NotNull private final TelemetryReporter reporter = new AppInsightsTelemetryReporter();

    public static @NotNull TelemetryService getInstance() {
        return ApplicationManager.getApplication().getService(TelemetryService.class);
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
}
