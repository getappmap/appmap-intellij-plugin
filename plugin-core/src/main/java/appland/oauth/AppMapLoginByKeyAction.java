package appland.oauth;

import appland.AppMapBundle;
import appland.notifications.AppMapNotifications;
import appland.settings.AppMapApplicationSettingsService;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.UpdateInBackground;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.InputValidator;
import com.intellij.openapi.ui.MessageMultilineInputDialog;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBTextArea;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.util.Objects;

import static com.intellij.openapi.ui.Messages.getCancelButton;
import static com.intellij.openapi.ui.Messages.getOkButton;

/**
 * Action to sign in to AppMap by providing a license key.
 */
public class AppMapLoginByKeyAction extends AnAction implements DumbAware, UpdateInBackground {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        var project = Objects.requireNonNull(e.getProject());

        var dialog = new KeyTextInputDialog(project,
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

    private static class KeyTextInputDialog extends MessageMultilineInputDialog {
        public KeyTextInputDialog(@NotNull Project project,
                                  @NotNull String message,
                                  @NotNull String title,
                                  @Nullable Icon icon,
                                  @Nullable @NonNls String initialValue,
                                  @Nullable InputValidator validator,
                                  String @NotNull [] options,
                                  int defaultOption) {
            super(project, message, title, icon, initialValue, validator, options, defaultOption);
        }

        @Override
        protected JTextComponent createTextFieldComponent() {
            var area = new JBTextArea(4, 50);
            area.setLineWrap(true);
            return area;
        }
    }
}
