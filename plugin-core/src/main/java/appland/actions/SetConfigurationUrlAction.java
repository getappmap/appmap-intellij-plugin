package appland.actions;

import appland.AppMapBundle;
import appland.enterpriseConfig.EnterpriseConfigService;
import appland.settings.AppMapApplicationSettingsService;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class SetConfigurationUrlAction extends AnAction implements DumbAware {
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        var project = e.getProject();
        showPicker(project);
    }

    public static void showPicker(@Nullable Project project) {
        var options = new String[]{
                AppMapBundle.get("action.appmap.setConfigurationUrl.option.url"),
                AppMapBundle.get("action.appmap.setConfigurationUrl.option.localFile"),
                Messages.getCancelButton()
        };

        var choice = Messages.showDialog(
                project,
                AppMapBundle.get("action.appmap.setConfigurationUrl.dialog.message"),
                AppMapBundle.get("action.appmap.setConfigurationUrl.dialog.title"),
                options,
                0,
                Messages.getQuestionIcon()
        );

        if (choice == 0) {
            // Set URL
            showUrlInputDialog(project);
        } else if (choice == 1) {
            // Local File
            showFileChooser(project);
        }
        // choice == 2 or -1 means cancel
    }

    private static void showUrlInputDialog(@Nullable Project project) {
        var applicationSettings = AppMapApplicationSettingsService.getInstance();
        var currentValue = applicationSettings.getConfigurationUrl();
        if (StringUtil.isEmpty(currentValue)) {
            currentValue = System.getenv("APPMAP_CONFIG_URL");
        }

        var result = Messages.showInputDialog(
                project,
                AppMapBundle.get("action.appmap.setConfigurationUrl.urlDialog.label"),
                AppMapBundle.get("action.appmap.setConfigurationUrl.urlDialog.title"),
                Messages.getQuestionIcon(),
                StringUtil.notNullize(currentValue),
                null
        );

        if (result != null) {
            // empty string clears the URL
            applicationSettings.setConfigurationUrlNotifying(StringUtil.nullize(result));
        }
    }

    private static void showFileChooser(@Nullable Project project) {
        var descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor("json");
        descriptor.setTitle(AppMapBundle.get("action.appmap.setConfigurationUrl.fileChooser.title"));

        var file = FileChooser.chooseFile(descriptor, project, null);
        if (file != null) {
            applyLocalFile(file, project);
        }
    }

    private static void applyLocalFile(@NotNull VirtualFile file, @Nullable Project project) {
        String content;
        try {
            content = new String(file.contentsToByteArray(), file.getCharset());
        } catch (IOException e) {
            Messages.showErrorDialog(project,
                    "Failed to read the configuration file: " + e.getMessage(),
                    AppMapBundle.get("action.appmap.setConfigurationUrl.dialog.title"));
            return;
        }
        EnterpriseConfigService.getInstance().applyLocalFile(content, project);
    }
}
