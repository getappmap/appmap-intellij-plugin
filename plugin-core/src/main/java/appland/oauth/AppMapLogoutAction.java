package appland.oauth;

import appland.settings.AppMapApplicationSettingsService;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.UpdateInBackground;
import com.intellij.openapi.project.DumbAware;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("ComponentNotRegistered")
public class AppMapLogoutAction extends AnAction implements DumbAware, UpdateInBackground {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        logout();
    }

    public static void logout() {
        AppMapApplicationSettingsService.getInstance().setApiKeyNotifying(null);
    }
}
