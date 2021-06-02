package appland.actions;

import appland.AppMapBundle;
import appland.Icons;
import appland.remote.RemoteRecordingService;
import appland.remote.RemoteRecordingStatusService;
import appland.remote.StopRemoteRecordingDialog;
import appland.settings.AppMapProjectSettingsService;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.vfs.LocalFileSystem;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Paths;

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

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        var project = e.getProject();
        assert project != null;

        var form = StopRemoteRecordingDialog.show(project);
        if (form == null) {
            return;
        }

        var parentDirPath = Paths.get(form.getDirectoryLocation());
        AppMapProjectSettingsService.getState(project).setRecentAppMapStorageLocation(parentDirPath.toString());

        new Task.Backgroundable(project, AppMapBundle.get("action.stopAppMapRemoteRecording.progressTitle"), false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                var newFile = RemoteRecordingService.getInstance().stopRecording(form.getURL(), parentDirPath, form.getName());
                if (newFile != null && Files.exists(newFile)) {
                    var newVfsFile = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(newFile);
                    if (newVfsFile != null) {
                        // open the new file in an editor
                        ApplicationManager.getApplication().invokeLater(() -> {
                            FileEditorManager.getInstance(project).openFile(newVfsFile, true, true);
                        }, ModalityState.defaultModalityState());
                    }
                }

                RemoteRecordingStatusService.getInstance(project).recordingStopped(form.getURL());
            }
        }.queue();
    }
}
