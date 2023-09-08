package appland.startup;

import appland.AppLandLifecycleService;
import appland.problemsView.FindingsManager;
import appland.settings.AppMapApplicationSettingsService;
import appland.settings.AppMapSettingsListener;
import appland.telemetry.TelemetryService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;

public class AppLandStartupActivity implements StartupActivity {
    @Override
    public void runActivity(@NotNull Project project) {
        // load initial findings of the project
        FindingsManager.getInstance(project).reloadAsync();

        registerFindingsTelemetryListener(project);
    }

    private void registerFindingsTelemetryListener(@NotNull Project project) {
        project.getMessageBus()
                .connect(AppLandLifecycleService.getInstance(project))
                .subscribe(AppMapSettingsListener.TOPIC, new AppMapSettingsListener() {
                    @Override
                    public void apiKeyChanged() {
                        sendAuthenticationTelemetry();
                    }

                    private void sendAuthenticationTelemetry() {
                        var eventName = AppMapApplicationSettingsService.getInstance().getApiKey() != null
                                ? "authentication:success"
                                : "authentication:sign_out";
                        TelemetryService.getInstance().sendEvent(eventName);
                    }
                });
    }
}
