package appland.webviews.appMap;

import appland.AppMapBundle;
import appland.Icons;
import appland.cli.AppLandCommandLineService;
import appland.notifications.AppMapNotifications;
import appland.webviews.WebviewEditorProvider;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class NavieEditorProvider extends WebviewEditorProvider {
    private static final String TYPE_ID = "appland.aiHelp";

    static final Key<String> KEY_QUESTION_TEXT = Key.create("appland.navie.text");
    static final Key<Integer> KEY_INDEXER_RPC_PORT = Key.create("appland.navie.rpcPort");

    public NavieEditorProvider() {
        super(TYPE_ID);
    }

    public static void openEditor(@NotNull Project project, @NotNull DataContext context) {
        var provider = WebviewEditorProvider.findEditorProvider(TYPE_ID);
        assert provider != null;

        var indexerPort = findIndexerJsonRpcPort(context);
        if (indexerPort == null) {
            AppMapNotifications.showNavieUnavailableNotification(project);
            return;
        }

        var editor = CommonDataKeys.EDITOR_EVEN_IF_INACTIVE.getData(context);

        var file = provider.createVirtualFile(AppMapBundle.get("webview.navie.title"));
        KEY_INDEXER_RPC_PORT.set(file, indexerPort);
        KEY_QUESTION_TEXT.set(file, editor != null ? editor.getSelectionModel().getSelectedText() : null);

        FileEditorManager.getInstance(project).openFile(file, true);
    }

    @Override
    public @NotNull FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile file) {
        return new NavieEditor(project, file);
    }

    @Override
    public @Nullable Icon getEditorIcon() {
        return Icons.APPMAP_FILE;
    }

    @Nullable
    private static Integer findIndexerJsonRpcPort(@NotNull DataContext context) {
        var contextFile = CommonDataKeys.VIRTUAL_FILE.getData(context);
        if (contextFile == null) {
            return null;
        }

        return AppLandCommandLineService.getInstance().getIndexerRpcPort(contextFile);
    }
}