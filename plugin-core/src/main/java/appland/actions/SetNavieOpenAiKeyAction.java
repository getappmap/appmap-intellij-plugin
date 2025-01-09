package appland.actions;

import appland.AppMapBundle;
import appland.settings.AppMapApplicationSettingsService;
import appland.settings.AppMapSecureApplicationSettingsService;
import appland.utils.WrappingTextInputDialog;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.concurrency.annotations.RequiresEdt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import static com.intellij.openapi.ui.Messages.getCancelButton;
import static com.intellij.openapi.ui.Messages.getOkButton;

public class SetNavieOpenAiKeyAction extends AnAction implements DumbAware {
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        showInputDialog(Objects.requireNonNull(e.getProject()));
    }

    @RequiresEdt
    public static void showInputDialog(@NotNull Project project) {
        var dialog = new WrappingTextInputDialog(project,
                AppMapBundle.get("action.appmap.navie.openAIKey.text.dialog.label"),
                AppMapBundle.get("action.appmap.navie.openAIKey.text.dialog.title"),
                null, null, null,
                new String[]{getOkButton(), getCancelButton()}, 0);
        dialog.show();

        var newKey = dialog.getInputString();
        if (StringUtil.isNotEmpty(newKey)) {
            AppMapApplicationSettingsService.resetCustomModelEnvironmentSettings();
            AppMapSecureApplicationSettingsService.getInstance().setOpenAIKey(newKey);
        }
    }
}
