package appland.actions;

import appland.Icons;
import appland.remote.RemoteRecordingService;
import appland.remote.StartRemoteRecordingDialog;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.RightAlignedToolbarAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAware;
import org.jetbrains.annotations.NotNull;

public class StartAppMapRecordingAction extends AnAction implements DumbAware {
    public StartAppMapRecordingAction() {
        super(Icons.START_RECORDING_ACTION);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        var project = e.getProject();
        assert project != null;

        var url = StartRemoteRecordingDialog.show(project
        );
        if (url != null) {
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                // fixme handle errors
                // fixme show success notification?
                RemoteRecordingService.getInstance().startRecording(url);
            });
        }
    }
}
