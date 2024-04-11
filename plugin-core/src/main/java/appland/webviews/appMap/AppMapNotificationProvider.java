package appland.webviews.appMap;

import appland.AppMapBundle;
import appland.files.AppMapFiles;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.ui.EditorNotificationProvider;
import com.intellij.ui.LightColors;
import com.intellij.ui.jcef.JBCefApp;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.function.Function;

public class AppMapNotificationProvider implements EditorNotificationProvider, DumbAware {
    @Override
    public @Nullable Function<? super @NotNull FileEditor, ? extends @Nullable JComponent> collectNotificationData(@NotNull Project project, @NotNull VirtualFile file) {
        return editor -> {
            if (AppMapFiles.isAppMap(file) && !JBCefApp.isSupported()) {
                var panel = new EditorNotificationPanel(LightColors.RED);
                panel.setText(AppMapBundle.get("appmap.editor.unavailableWarning"));
                return panel;
            }

            return null;
        };
    }
}
