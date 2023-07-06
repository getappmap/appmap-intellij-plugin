package appland.webviews;

import appland.Icons;
import appland.files.AppMapFiles;
import appland.installGuide.InstallGuideEditorProvider;
import com.intellij.ide.FileIconProvider;
import com.intellij.openapi.fileEditor.ex.FileEditorProviderManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class WebviewEditorIconProvider implements FileIconProvider {
    @Override
    public @Nullable Icon getIcon(@NotNull VirtualFile file, int flags, @Nullable Project project) {
        if (AppMapFiles.isAppMap(file)) {
            return Icons.APPMAP_FILE_SMALL;
        }

        // special handling for the install guide webview until we've migrated it to WebviewEditorProvider
        if (InstallGuideEditorProvider.isInstallGuideFile(file)) {
            return Icons.APPMAP_FILE_SMALL;
        }

        var editorProviderId = WebviewEditorProvider.WEBVIEW_EDITOR_KEY.get(file);
        if (editorProviderId != null) {
            // we can't use FileEditorProviderManager.getInstance().getProvider(editorProviderId)
            // because it turned from class to interface in 2023.1.
            // Using it would raise an error with plugin verifier.

            return Icons.APPMAP_FILE_SMALL;

            /*
            var provider = FileEditorProviderManager.getInstance().getProvider(editorProviderId);
            if (provider instanceof WebviewEditorProvider) {
                return ((WebviewEditorProvider) provider).getEditorIcon();
            }
            */
        }

        return null;
    }
}
