package appland.oauth;

import appland.settings.AppMapApplicationSettingsService;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.UpdateInBackground;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbAware;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("ComponentNotRegistered")
public class AppMapLoginAction extends AnAction implements DumbAware, UpdateInBackground {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        authenticate();
    }

    public static void authenticate() {
        var response = AppMapOAuthService.getInstance().authorize();
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                var responseData = response.get();
                var settings = AppMapApplicationSettingsService.getInstance();
                settings.setApiKeyNotifying(responseData.getAccessToken());
            } catch (Exception ex) {
                Logger.getInstance(AppMapLoginAction.class).warn("Error authenticating with AppMap server", ex);
            }
        });
    }
}
