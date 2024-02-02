package appland.remote;

import appland.AppMapBundle;
import appland.settings.AppMapProjectSettingsService;
import appland.validator.UrlInputValidator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Dialog to enter the data to start a new remote recording.
 */
public class StartRemoteRecordingDialog extends DialogWrapper {
    private final StartRemoteRecordingForm form;

    protected StartRemoteRecordingDialog(@NotNull Project project) {
        super(project);

        var recentURLs = AppMapProjectSettingsService.getState(project).getRecentRemoteRecordingURLs();
        form = new StartRemoteRecordingForm(recentURLs);

        init();
    }

    /**
     * Show the dialog and return the entered URL.
     *
     * @param project The current project
     * @return The URL of {@code null} if the dialog was cancelled.
     */
    @Nullable
    public static String show(@NotNull Project project) {

        var dialog = new StartRemoteRecordingDialog(project);
        dialog.init();
        dialog.setTitle(AppMapBundle.get("action.startAppMapRemoteRecording.dialogTitle"));
        dialog.setOKButtonText(AppMapBundle.get("action.startAppMapRemoteRecording.startButton"));
        if (!dialog.showAndGet()) {
            return null;
        }

        var url = dialog.form.getUrl();
        AppMapProjectSettingsService.getState(project).addRecentRemoteRecordingURLs(url);
        return url;
    }

    @Override
    protected @Nullable ValidationInfo doValidate() {
        var urlError = UrlInputValidator.INSTANCE.getErrorText(form.getUrl());
        if (urlError != null) {
            return new ValidationInfo(urlError, form.getUrlComboBox());
        }

        return null;
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return form.getUrlComboBox();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return form.getMainPanel();
    }
}
