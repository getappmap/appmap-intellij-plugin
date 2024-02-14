package appland.webviews.navie;

import appland.AppMapBundle;
import appland.Icons;
import appland.cli.AppLandCommandLineService;
import appland.notifications.AppMapNotifications;
import appland.webviews.WebviewEditorProvider;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiManager;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Open the Navie webview in a new tab.
 */
public final class NavieEditorProvider extends WebviewEditorProvider {
    private static final String TYPE_ID = "appland.navie";

    /**
     * Default text for the question text input box.
     */
    static final Key<NavieCodeSelection> KEY_CODE_SELECTION = Key.create("appland.navie.codeSelection");
    /**
     * The port of the matching indexer's JSON-RPC service.
     */
    static final Key<Integer> KEY_INDEXER_RPC_PORT = Key.create("appland.navie.rpcPort");

    public NavieEditorProvider() {
        super(TYPE_ID);
    }

    /**
     * Open a new Navie webview with indexer port and selected derived from the current DataContext.
     * If no indexer port could be found for the context, an error message is displayed.
     *
     * @param project Current project
     * @param context Data context to locate matching indexer and selected text, if available.
     */
    public static void openEditor(@NotNull Project project, @NotNull DataContext context) {
        var provider = WebviewEditorProvider.findEditorProvider(TYPE_ID);
        assert provider != null;

        var editor = CommonDataKeys.EDITOR_EVEN_IF_INACTIVE.getData(context);
        chooseIndexerJsonRpcPort(project, context, editor, (indexerPort, codeSelection) -> {
            if (indexerPort == null) {
                AppMapNotifications.showNavieUnavailableNotification(project);
                return;
            }

            var file = provider.createVirtualFile(AppMapBundle.get("webview.navie.title"));
            KEY_INDEXER_RPC_PORT.set(file, indexerPort);
            KEY_CODE_SELECTION.set(file, codeSelection);

            FileEditorManager.getInstance(project).openFile(file, true);
        });
    }

    private static @Nullable NavieCodeSelection buildCodeSelection(@NotNull Project project, @Nullable Editor editor) {
        var selection = editor != null ? editor.getSelectionModel() : null;
        if (selection == null || !selection.hasSelection()) {
            return null;
        }

        var document = selection.getEditor().getDocument();
        var fileIndex = ProjectFileIndex.getInstance(project);
        var virtualFile = FileDocumentManager.getInstance().getFile(selection.getEditor().getDocument());
        var rootFile = virtualFile != null ? fileIndex.getContentRootForFile(virtualFile) : null;
        var psiFile = virtualFile != null ? PsiManager.getInstance(project).findFile(virtualFile) : null;

        String relativePath = null;
        if (rootFile != null && virtualFile != null) {
            relativePath = VfsUtil.getRelativePath(virtualFile, rootFile);
        }

        return new NavieCodeSelection(selection.getSelectedText(),
                relativePath,
                document.getLineNumber(selection.getSelectionStart()) + 1,
                document.getLineNumber(selection.getSelectionEnd()) + 1,
                psiFile != null ? psiFile.getLanguage().getID() : null);
    }

    private static void chooseIndexerJsonRpcPort(@NotNull Project project,
                                                 @NotNull DataContext context,
                                                 @Nullable Editor editor,
                                                 @NotNull ShowNavieConsumer consumer) {
        var service = AppLandCommandLineService.getInstance();
        var codeSelection = buildCodeSelection(project, editor);

        var activeRoots = service.getActiveRoots();
        if (activeRoots.isEmpty()) {
            consumer.openNavie(null, null);
            return;
        }

        // without a context file, fallback to the only available indexer port if there's exactly one active root
        if (activeRoots.size() == 1) {
            consumer.openNavie(service.getIndexerRpcPort(activeRoots.get(0)), codeSelection);
            return;
        }

        // for multiple active roots, let the user choose one
        var selectPortStep = new BaseListPopupStep<>(AppMapBundle.get("webview.navie.chooseAppMapModule.title"), activeRoots) {
            @Override
            public @NotNull String getTextFor(VirtualFile file) {
                var projectDir = ProjectUtil.guessProjectDir(project);
                var relativePath = projectDir != null ? VfsUtil.getRelativePath(file, projectDir) : null;
                return StringUtil.defaultIfEmpty(relativePath, file.getPresentableUrl());
            }

            @Override
            public @Nullable PopupStep<?> onChosen(VirtualFile selectedValue, boolean finalChoice) {
                var port = selectedValue != null ? service.getIndexerRpcPort(selectedValue) : null;
                consumer.openNavie(port, codeSelection);
                return FINAL_CHOICE;
            }
        };

        JBPopupFactory.getInstance().createListPopup(selectPortStep).showCenteredInCurrentWindow(project);
    }

    @Override
    public @NotNull FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile file) {
        return new NavieEditor(project, file);
    }

    @Override
    public Icon getEditorIcon() {
        return Icons.APPMAP_FILE;
    }

    @FunctionalInterface
    private interface ShowNavieConsumer {
        void openNavie(@Nullable Integer indexerPort, @Nullable NavieCodeSelection codeSelection);
    }
}