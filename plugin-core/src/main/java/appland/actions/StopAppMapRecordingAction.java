package appland.actions;

import appland.AppMapBundle;
import appland.Icons;
import appland.notifications.AppMapNotifications;
import appland.remote.RemoteRecordingService;
import appland.remote.RemoteRecordingStatusService;
import appland.remote.StopRemoteRecordingDialog;
import appland.remote.StopRemoteRecordingForm;
import appland.settings.AppMapProjectSettingsService;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicReference;

public class StopAppMapRecordingAction extends AnAction implements DumbAware {
    private static final Logger LOG = Logger.getInstance(StopAppMapRecordingAction.class);

    public StopAppMapRecordingAction() {
        super(Icons.STOP_RECORDING_ACTION);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        var project = e.getProject();
        if (project == null) {
            return;
        }

        // don't hide the "Stop recording" action in the global list, only in the toolbar
        // we still want to allow a user to stop recording at any URL
        if (e.isFromActionToolbar()) {
            var recording = RemoteRecordingStatusService.getInstance(project).getActiveRecordingURL() != null;
            e.getPresentation().setEnabledAndVisible(recording);
        }
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        var project = e.getProject();
        assert project != null;

        new Task.Modal(project, AppMapBundle.get("action.stopAppMapRemoteRecording.locationProgress.title"), true) {
            private final AtomicReference<String> location = new AtomicReference<>();

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                if (project.isDisposed()) {
                    return;
                }

                indicator.setText(AppMapBundle.get("action.stopAppMapRemoteRecording.locationProgress.progressTitle"));
                location.set(findDefaultStorageLocation(project));
            }

            @Override
            public void onSuccess() {
                if (project.isDisposed()) {
                    return;
                }

                var form = StopRemoteRecordingDialog.show(project,
                        location.get(),
                        RemoteRecordingStatusService.getInstance(project).getActiveRecordingURL(),
                        AppMapProjectSettingsService.getState(project).getRecentRemoteRecordingURLs());
                if (form != null) {
                    stopAndSaveRemoteRecording(project, form);
                }
            }
        }.queue();
    }

    private static void stopAndSaveRemoteRecording(@NotNull Project project, @NotNull StopRemoteRecordingForm form) {
        var storageDirectoryPath = form.getDirectoryLocation();
        AppMapProjectSettingsService.getState(project).setRecentAppMapStorageLocation(storageDirectoryPath);

        new Task.Backgroundable(project, AppMapBundle.get("action.stopAppMapRemoteRecording.progressTitle"), false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                Path nioStoragePath;
                try {
                    nioStoragePath = Paths.get(storageDirectoryPath);
                } catch (InvalidPathException e) {
                    LOG.debug("Invalid storage location", e);
                    showStopRecordingFailedError(project, form.getURL());
                    return;
                }

                var newFile = RemoteRecordingService.getInstance().stopRecording(form.getURL(), nioStoragePath, form.getName());
                RemoteRecordingStatusService.getInstance(project).recordingStopped(form.getURL());

                if (newFile != null && Files.exists(newFile)) {
                    var newVfsFile = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(newFile);
                    if (newVfsFile != null) {
                        // open the new file in an editor
                        ApplicationManager.getApplication().invokeLater(() -> {
                            FileEditorManager.getInstance(project).openFile(newVfsFile, true, true);
                        }, ModalityState.defaultModalityState());
                    }
                } else {
                    showStopRecordingFailedError(project, form.getURL());
                }
            }
        }.queue();
    }

    /**
     * try to find a reasonable default for the storage location
     */
    private static @Nullable String findDefaultStorageLocation(@NotNull Project project) {
        var state = AppMapProjectSettingsService.getState(project);

        var storageLocation = state.getRecentAppMapStorageLocation();
        if (StringUtil.isEmpty(storageLocation)) {
            var projectDir = ProjectUtil.guessProjectDir(project);
            if (projectDir != null) {
                var nioProjectDir = projectDir.getFileSystem().getNioPath(projectDir);
                storageLocation = nioProjectDir != null ? nioProjectDir.toString() : "";
            }
        }
        return storageLocation;
    }

    private static void showStopRecordingFailedError(@NotNull Project project, @NotNull String url) {
        AppMapNotifications.showExpandedRecordingNotification(project,
                AppMapBundle.get("notification.recordingStopFailed.title"),
                AppMapBundle.get("notification.recordingStopFailed.content", url),
                NotificationType.ERROR, true, false, true);
    }
}
