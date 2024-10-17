package appland.actions;

import appland.webviews.navie.NavieEditor;
import com.intellij.openapi.project.Project;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Action to let the user choose the files to add as pinned files to the active Navie editor.
 */
public class ChooseAndAddNavieContextFilesAction extends AnAction {
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(findActiveNavieEditor(e) != null);
    }

    public static void chooseAndAddPinnedFiles(Project project, NavieEditor editor) {
        var descriptor = new FileChooserDescriptor(true, false, false, false, false, true);
        var chosenFiles = FileChooser.chooseFiles(descriptor, project, null);
        if (chosenFiles.length > 0) {
            editor.addPinnedFiles(List.of(chosenFiles));
        }
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        var editor = findActiveNavieEditor(e);
        if (editor != null) {
            chooseAndAddPinnedFiles(e.getProject(), editor);
        }
    }

    private @Nullable NavieEditor findActiveNavieEditor(@NotNull AnActionEvent e) {
        var project = e.getProject();
        var editor = project != null ? FileEditorManager.getInstance(project).getSelectedEditor() : null;
        return editor instanceof NavieEditor ? (NavieEditor) editor : null;
    }
}
