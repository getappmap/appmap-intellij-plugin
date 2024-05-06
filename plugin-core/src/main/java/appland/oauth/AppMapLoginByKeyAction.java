package appland.oauth;

import appland.AppMapBundle;
import appland.notifications.AppMapNotifications;
import appland.settings.AppMapApplicationSettingsService;
import appland.utils.WrappingTextInputDialog;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static com.intellij.openapi.ui.Messages.getCancelButton;
import static com.intellij.openapi.ui.Messages.getOkButton;

/**
 * Action to sign in to AppMap by providing a license key.
 */
public class AppMapLoginByKeyAction extends AnAction implements DumbAware {
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        var project = Objects.requireNonNull(e.getProject());

        var dialog = new WrappingTextInputDialog(project,
                AppMapBundle.get("action.appMapLoginByKey.dialog.label"),
                AppMapBundle.get("action.appMapLoginByKey.dialog.title"),
                null, null, new AppMapLocalKeyValidator(),
                new String[]{getOkButton(), getCancelButton()}, 0);
        dialog.show();

        var newKey = dialog.getInputString();
        if (newKey != null) {
            if (!new AppMapKeyRemoteValidator(project).checkInput(newKey)) {
                var title = AppMapBundle.get("action.appMapLoginByKey.dialog.invalidKeyErrorMessage.title");
                var message = AppMapBundle.get("action.appMapLoginByKey.dialog.invalidKeyErrorMessage.message");
                Messages.showErrorDialog(project, message, title);
                return;
            }

            AppMapApplicationSettingsService.getInstance().setApiKeyNotifying(newKey);
            AppMapNotifications.showSignInNotification(project);
        }
    }
}
