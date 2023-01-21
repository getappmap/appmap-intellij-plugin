package appland.actions;

import appland.Icons;
import appland.notifications.AppMapNotifications;
import appland.remote.RemoteRecordingService;
import appland.remote.RemoteRecordingStatusService;
import appland.remote.StopRemoteRecordingDialog;
import appland.settings.AppMapProjectSettings;
import appland.settings.AppMapProjectSettingsService;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
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
import java.nio.file.Paths;

import static appland.AppMapBundle.get;

public class StopAppMapRecordingAction extends AnAction implements DumbAware {
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

    /**
     * try to find a reasonable default for the storage location
     */
    private static @Nullable String findDefaultStorageLocation(@NotNull Project project,
                                                               @NotNull AppMapProjectSettings state) {
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

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        var project = e.getProject();
        assert project != null;

        var state = AppMapProjectSettingsService.getState(project);
        var form = StopRemoteRecordingDialog.show(project,
                findDefaultStorageLocation(project, state),
                RemoteRecordingStatusService.getInstance(project).getActiveRecordingURL(),
                state.getRecentRemoteRecordingURLs()
        );
        if (form == null) {
            // user cancelled the form
            return;
        }

        var parentDirPath = Paths.get(form.getDirectoryLocation());
        state.setRecentAppMapStorageLocation(parentDirPath.toString());

        new Task.Backgroundable(project, get("action.stopAppMapRemoteRecording.progressTitle"), false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                var newFile = RemoteRecordingService.getInstance().stopRecording(form.getURL(), parentDirPath, form.getName());
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
                    AppMapNotifications.showExpandedRecordingNotification(project,
                            get("notification.recordingStopFailed.title"),
                            get("notification.recordingStopFailed.content", form.getURL()),
                            NotificationType.ERROR, true, false, true);
                }
            }
        }.queue();
    }
}
