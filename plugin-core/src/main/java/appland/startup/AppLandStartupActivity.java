package appland.startup;

import appland.AppLandLifecycleService;
import appland.installGuide.InstallGuideEditorProvider;
import appland.installGuide.InstallGuideViewPage;
import appland.problemsView.FindingsManager;
import appland.settings.AppMapApplicationSettingsService;
import appland.settings.AppMapSettingsListener;
import appland.telemetry.TelemetryService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;

public class AppLandStartupActivity implements StartupActivity {
    @Override
    public void runActivity(@NotNull Project project) {
        // load initial findings of the project
        FindingsManager.getInstance(project).reloadAsync();

        var projectMessageBus = project.getMessageBus().connect(AppLandLifecycleService.getInstance(project));
        registerFindingsTelemetryListener(projectMessageBus);
        registerShowInstructionsAfterSignIn(project, projectMessageBus);
    }

    private void registerFindingsTelemetryListener(@NotNull MessageBusConnection projectMessageBus) {
        projectMessageBus.subscribe(AppMapSettingsListener.TOPIC, new AppMapSettingsListener() {
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

    private void registerShowInstructionsAfterSignIn(@NotNull Project project,
                                                     @NotNull MessageBusConnection projectMessageBus) {
        projectMessageBus.subscribe(AppMapSettingsListener.TOPIC, new AppMapSettingsListener() {
            @Override
            public void apiKeyChanged() {
                var settings = AppMapApplicationSettingsService.getInstance();
                if (!settings.isInstallInstructionsViewed() && settings.isAuthenticated()) {
                    settings.setInstallInstructionsViewed(true);

                    var app = ApplicationManager.getApplication();
                    if (!app.isUnitTestMode()) {
                        app.invokeLater(() -> {
                            InstallGuideEditorProvider.open(project, InstallGuideViewPage.InstallAgent);
                        }, ModalityState.defaultModalityState());
                    }
                }
            }
        });
    }
}
