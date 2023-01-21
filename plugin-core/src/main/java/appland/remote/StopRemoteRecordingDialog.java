package appland.remote;

import appland.AppMapBundle;
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
                                        @Nullable String defaultStorageLocation,
                                        @Nullable String activeRecordingUrl,
                                        @NotNull List<String> recentRecordingUrls) {
        super(project);

        form = new StopRemoteRecordingForm(project, defaultStorageLocation, activeRecordingUrl, recentRecordingUrls);
        init();
    }

    /**
     * Show the dialog and return the entered URL.
     *
     * @param project                The current project
     * @param defaultStorageLocation Default value for the "storage location" input
     * @param activeRecordingUrl     Default for the "Remote URL" input
     * @param recentRecordingUrls    Values for the "Remote URL" dropdown
     * @return The URL of {@code null} if the dialog was cancelled.
     */
    @Nullable
    public static StopRemoteRecordingForm show(@NotNull Project project,
                                               @Nullable String defaultStorageLocation,
                                               @Nullable String activeRecordingUrl,
                                               @NotNull List<String> recentRecordingUrls) {
        var dialog = new StopRemoteRecordingDialog(project, defaultStorageLocation, activeRecordingUrl, recentRecordingUrls);
        dialog.init();
        dialog.setTitle(AppMapBundle.get("action.stopAppMapRemoteRecording.dialogTitle"));
        dialog.setOKButtonText(AppMapBundle.get("action.stopAppMapRemoteRecording.stopButton"));
        return dialog.showAndGet() ? dialog.form : null;
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
            var locationPath = Paths.get(form.getDirectoryLocation());
            // if the directory does not yet exist, then we ignore it because the save implementation will create it
            if (Files.exists(locationPath)) {
                if (!Files.isDirectory(locationPath)) {
                    return new ValidationInfo(AppMapBundle.get("dirPathValidation.notDir"), form.getDirectoryLocationInput());
                }
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
