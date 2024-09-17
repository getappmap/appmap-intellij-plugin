package appland.actions;

import appland.webviews.navie.NavieEditor;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Action to add VirtualFiles available in the action context as pinned files to the currently active Navie editor.
 */
public class AddNavieContextFilesAction extends AnAction {
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(findActiveNavieEditor(e) != null && hasSelectedFiles(e));
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        var editor = findActiveNavieEditor(e);
        if (editor != null) {
            editor.addPinnedFiles(findSelectedFiles(e));
        }
    }

    private boolean hasSelectedFiles(@NotNull AnActionEvent e) {
        var files = PlatformDataKeys.VIRTUAL_FILE_ARRAY.getData(e.getDataContext());
        if (files != null) {
            for (var file : files) {
                if (!file.isDirectory() && file.isInLocalFileSystem()) {
                    return true;
                }
            }
        }

        return false;
    }

    private @NotNull List<VirtualFile> findSelectedFiles(@NotNull AnActionEvent e) {
        var files = PlatformDataKeys.VIRTUAL_FILE_ARRAY.getData(e.getDataContext());
        if (files == null || files.length == 0) {
            return Collections.emptyList();
        }

        // we're only accepting files on the local filesystem, the file size is validated by NavieEditor
        var localFiles = new ArrayList<VirtualFile>();
        for (var file : files) {
            if (!file.isDirectory() && file.isInLocalFileSystem()) {
                localFiles.add(file);
            }
        }
        return localFiles;
    }

    private @Nullable NavieEditor findActiveNavieEditor(@NotNull AnActionEvent e) {
        var editorManager = FileEditorManager.getInstance(Objects.requireNonNull(e.getProject()));
        var editor = (editorManager).getSelectedEditor();

        // If invoked with the context menu of an editor tab, then "editor" equals the clicked tab's editor and not the
        // visible Navie editor tab. We're attempting a fallback in such a case.
        if (ActionPlaces.EDITOR_TAB_POPUP.equals(e.getPlace()) && !(editor instanceof NavieEditor)) {
            for (var candidate : editorManager.getAllEditors()) {
                if (candidate instanceof NavieEditor) {
                    editor = candidate;
                    break;
                }
            }
        }

        return editor instanceof NavieEditor ? (NavieEditor) editor : null;
    }
}
