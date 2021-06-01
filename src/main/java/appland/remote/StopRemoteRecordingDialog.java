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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Dialog to enter the data to stop a remote recording.
 */
public class StopRemoteRecordingDialog extends DialogWrapper {
    private final StopRemoteRecordingForm form;

    protected StopRemoteRecordingDialog(@NotNull Project project,
                                        @NotNull List<String> recentURLs,
                                        @NotNull String lastLocation) {
        super(project);
        form = new StopRemoteRecordingForm(project, recentURLs, lastLocation);
        init();
    }

    /**
     * Show the dialog and return the entered URL.
     *
     * @param project The current project
     * @return The URL of {@code null} if the dialog was cancelled.
     */
    @Nullable
    public static StopRemoteRecordingForm show(@NotNull Project project) {
        var recentURLs = AppMapProjectSettingsService.getState(project).getRecentRemoteRecordingURLs();
        var lastLocation = AppMapProjectSettingsService.getState(project).getRecentAppMapStorageLocation();

        var dialog = new StopRemoteRecordingDialog(project, recentURLs, lastLocation);
        dialog.init();
        dialog.setTitle(AppMapBundle.get("action.stopAppMapRemoteRecording.dialogTitle"));
        dialog.setOKButtonText(AppMapBundle.get("action.stopAppMapRemoteRecording.stopButton"));
        if (recentURLs.size() > 0) {
            dialog.form.getUrlComboBox().setText(recentURLs.get(0));
        }

        if (!dialog.showAndGet()) {
            return null;
        }

        return dialog.form;
    }

    @Override
    protected @Nullable ValidationInfo doValidate() {
        var urlError = UrlInputValidator.INSTANCE.getErrorText(form.getURL());
        if (urlError != null) {
            return new ValidationInfo(urlError, form.getUrlComboBox());
        }

        if (form.getName().isBlank()) {
            return new ValidationInfo(AppMapBundle.get("nameValidation.empty"), form.getAppMapNameInput());
        }

        if (form.getDirectoryLocation().isBlank()) {
            return new ValidationInfo(AppMapBundle.get("dirPathValidation.empty"), form.getDirectoryLocationInput());
        }

        try {
            if (!Files.isDirectory(Paths.get(form.getDirectoryLocation()))) {
                return new ValidationInfo(AppMapBundle.get("dirPathValidation.notDir"), form.getDirectoryLocationInput());
            }
        } catch (Exception e) {
            return new ValidationInfo(AppMapBundle.get("dirPathValidation.invalidPath"), form.getDirectoryLocationInput());
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
