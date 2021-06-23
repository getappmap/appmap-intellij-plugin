package appland.actions;

import appland.Icons;
import appland.files.AppMapFiles;
import appland.upload.AppMapUploader;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Action to upload the currently opened AppMap file.
 */
public class UploadAppMapAction extends AnAction implements DumbAware {
    public UploadAppMapAction() {
        super(Icons.APPMAP_FILE);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        var file = getFile(e);
        e.getPresentation().setEnabled(file != null && AppMapFiles.isAppMap(file));
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        var project = e.getProject();
        var file = getFile(e);
        if (project == null || file == null || !AppMapFiles.isAppMap(file)) {
            return;
        }

        AppMapUploader.uploadAppMap(project, file);
    }

    @Nullable
    private VirtualFile getFile(@NotNull AnActionEvent e) {
        return e.getData(CommonDataKeys.VIRTUAL_FILE);
    }
}
