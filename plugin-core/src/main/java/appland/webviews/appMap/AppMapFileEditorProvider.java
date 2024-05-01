package appland.webviews.appMap;

import appland.Icons;
import appland.files.AppMapFiles;
import appland.settings.AppMapProjectSettingsService;
import appland.webviews.WebviewEditorProvider;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.concurrency.annotations.RequiresEdt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Provides the HTML JCEF component instead of the default JSON editor for files ending with ".appmap.json".
 */
public class AppMapFileEditorProvider extends WebviewEditorProvider {
    private static final String TYPE_ID = "appland.appMap";

    public AppMapFileEditorProvider() {
        super(TYPE_ID);
    }

    /**
     * Opens the given file in the AppMap editor.
     * If {@code editorState} is given, then this state will be applied to the editor.
     *
     * @param project     Current project
     * @param virtualFile AppMap file to open
     * @param editorState Optional state to apply to the new editor
     */
    @RequiresEdt
    public static void openAppMap(@NotNull Project project,
                                  @NotNull VirtualFile virtualFile,
                                  @Nullable AppMapFileEditorState editorState) {
        var appMapEditors = FileEditorManager.getInstance(project).openFile(virtualFile, true);
        if (appMapEditors.length == 1 && appMapEditors[0] instanceof AppMapFileEditor && editorState != null) {
            ((AppMapFileEditor) appMapEditors[0]).setWebViewState(editorState);
        }
    }

    @Override
    public @Nullable Icon getEditorIcon() {
        return Icons.APPMAP_FILE;
    }

    @Override
    public boolean isWebViewFile(@NotNull VirtualFile file) {
        return AppMapFiles.isAppMap(file);
    }

    @Override
    public @NotNull FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile file) {
        AppMapProjectSettingsService.getState(project).setOpenedAppMapEditor(true);
        return new AppMapFileEditor(project, file);
    }
}
