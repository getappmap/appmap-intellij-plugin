package appland.webviews.appMap;

import appland.Icons;
import appland.files.AppMapFiles;
import appland.settings.AppMapProjectSettingsService;
import appland.webviews.WebviewEditorProvider;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Provides the HTML JCEF component instead of the default JSON editor for files ending with ".appmap.json".
 */
public class AppMapHTMLEditorProvider extends WebviewEditorProvider {
    private static final String TYPE_ID = "appland.appMap";

    public AppMapHTMLEditorProvider() {
        super(TYPE_ID);
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
