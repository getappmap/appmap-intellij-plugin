package appland.webviews.findings;

import appland.AppMapBundle;
import appland.Icons;
import appland.webviews.WebviewEditorProvider;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class FindingsOverviewEditorProvider extends WebviewEditorProvider {
    private static final String TYPE_ID = "appland.findingsOverview";

    public FindingsOverviewEditorProvider() {
        super(TYPE_ID);
    }

    public static void openEditor(@NotNull Project project) {
        var provider = WebviewEditorProvider.findEditorProvider(TYPE_ID);
        assert provider != null;
        provider.open(project, AppMapBundle.get("webview.findingsOverview.title"));
    }

    @Override
    public @NotNull FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile file) {
        return new FindingsOverviewEditor(project, file);
    }

    @Override
    public @Nullable Icon getEditorIcon() {
        return Icons.APPMAP_FILE;
    }
}
