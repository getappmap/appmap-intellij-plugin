package appland.webviews.appMap;

import appland.AppMapBundle;
import appland.files.AppMapFiles;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.ui.EditorNotifications;
import com.intellij.ui.LightColors;
import com.intellij.ui.jcef.JBCefApp;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AppMapNotificationProvider extends EditorNotifications.Provider<EditorNotificationPanel> implements DumbAware {
    private static final Key<EditorNotificationPanel> KEY = Key.create("appland.jcefEditor");

    @Override
    public @NotNull Key<EditorNotificationPanel> getKey() {
        return KEY;
    }

    @Nullable
    @Override
    public EditorNotificationPanel createNotificationPanel(@NotNull VirtualFile file, @NotNull FileEditor fileEditor, @NotNull Project project) {
        if (AppMapFiles.isAppMap(file) && !JBCefApp.isSupported()) {
            var panel = new EditorNotificationPanel(LightColors.RED);
            panel.setText(AppMapBundle.get("appmap.editor.unavailableWarning"));
            return panel;
        }

        return null;
    }
}
