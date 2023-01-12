package appland.webviews.appMap;

import appland.Icons;
import appland.files.AppMapFiles;
import appland.installGuide.InstallGuideEditorProvider;
import appland.webviews.WebviewEditorProvider;
import com.intellij.ide.FileIconProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class AppMapIconProvider implements FileIconProvider {
    @Override
    @Nullable
    public Icon getIcon(@NotNull VirtualFile file, int flags, @Nullable Project project) {
        // generic webview editor
        var webviewTypeId = WebviewEditorProvider.WEBVIEW_EDITOR_KEY.get(file);
        if (webviewTypeId != null) {
            var editorProvider = WebviewEditorProvider.findEditorProvider(webviewTypeId);
            if (editorProvider != null) {
                return editorProvider.getEditorIcon();
            }
        }

        return AppMapFiles.isAppMap(file) || InstallGuideEditorProvider.isInstallGuideFile(file)
                ? Icons.APPMAP_FILE
                : null;
    }
}
