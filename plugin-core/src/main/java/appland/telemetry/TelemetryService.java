package appland.telemetry;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;

import appland.AppMapBundle;
import appland.notifications.AppMapNotifications;
import appland.settings.AppMapApplicationSettingsService;
import appland.telemetry.data.BaseData;
import appland.telemetry.data.TelemetryEvent;

import org.jetbrains.annotations.NotNull;

public class TelemetryService {
    @NotNull private final String instrumentationKey = "50c1a5c2-49b4-4913-b7cc-86a78d40745f";
    @NotNull private final String ingestionEndpoint = "https://centralus-0.in.applicationinsights.azure.com";
    @NotNull private final String eventPrefix = "appland.appmap/";
    @NotNull protected final AppInsightsClient client = new AppInsightsClient(ingestionEndpoint);

    public static @NotNull TelemetryService getInstance() {
        return ApplicationManager.getApplication().getService(TelemetryService.class);
    }

    public interface TelemetryEventBuilder {
        BaseData build(@NotNull BaseData eventData);
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

    /**
     * Sends data to the telemetry server. The request is sent in a background thread.
     *
     * @param event Tracked event
     */
    private void sendEvent(@NotNull TelemetryEvent event) {
        if (!isEnabled()) {
            return;
        }

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            client.track(event);
        });
    }

    public void sendEvent(@NotNull String name, @NotNull TelemetryEventBuilder builder) {
        var data = builder.build(new BaseData(eventPrefix + name));
        var event = new TelemetryEvent(instrumentationKey, data);
        sendEvent(event);
    }

    public void sendEvent(@NotNull String name) {
        var event = new TelemetryEvent(instrumentationKey, new BaseData(eventPrefix + name));
        sendEvent(event);
    }
}
