package appland.oauth;

import appland.settings.AppMapApplicationSettingsService;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("ComponentNotRegistered")
public class AppMapLogoutAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        logout();
    }

    public static void logout() {
        AppMapApplicationSettingsService.getInstance().setApiKey(null);
    }
}
