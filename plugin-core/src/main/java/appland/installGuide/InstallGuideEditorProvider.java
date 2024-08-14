package appland.installGuide;

import appland.AppMapBundle;
import appland.Icons;
import appland.webviews.WebviewEditorProvider;
import com.google.common.base.Predicates;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class InstallGuideEditorProvider extends WebviewEditorProvider {
    private static final String TYPE_ID = "appland.installGuide";

    // defines the install-guide view type, i.e. the install-guide page to show in a particular editor
    private static final Key<InstallGuideViewPage> INSTALL_GUIDE_PAGE_KEY = Key.create("appland.installGuideFile");

    /**
     * Open a new Install Guide webview at the given page.
     * An existing Install Guide webview is updated to navigate to the requested page.
     */
    public static void open(@NotNull Project project, @NotNull InstallGuideViewPage page) {
        try {
            var provider = WebviewEditorProvider.findEditorProvider(TYPE_ID);
            assert provider != null;

            // an open Install Guide webview must navigate to the new page
            var openEditor = provider.findOpenEditor(project, Predicates.alwaysTrue());
            if (openEditor != null) {
                return;
            }

            var file = provider.createVirtualFile(AppMapBundle.get("installGuide.editor.title"));
            INSTALL_GUIDE_PAGE_KEY.set(file, page);
            FileEditorManager.getInstance(project).openFile(file, true);
        } finally {
            // notify in a background thread because we don't want to delay opening the editor
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                project.getMessageBus().syncPublisher(InstallGuideListener.TOPIC).afterInstallGuidePageOpened(page);
            });
        }
    }

    public InstallGuideEditorProvider() {
        super(TYPE_ID);
    }

    @Override
    public @Nullable Icon getEditorIcon() {
        return Icons.APPMAP_FILE_SMALL;
    }

    @Override
    public @NotNull FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile file) {
        var page = INSTALL_GUIDE_PAGE_KEY.get(file);
        return new InstallGuideEditor(project, file);
    }
}
