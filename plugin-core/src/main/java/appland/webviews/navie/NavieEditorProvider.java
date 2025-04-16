package appland.webviews.navie;

import appland.AppMapBundle;
import appland.Icons;
import appland.notifications.AppMapNotifications;
import appland.rpcService.AppLandJsonRpcService;
import appland.settings.AppMapProjectSettingsService;
import appland.utils.DataContexts;
import appland.webviews.WebviewEditorProvider;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DataKey;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.concurrency.annotations.RequiresEdt;
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
    /**
     * Optional AppMap context passed to the Navie webview.
     */
    static final Key<VirtualFile> KEY_APPMAP_CONTEXT_FILE = Key.create("appland.navie.appmap");
    /**
     * Optional prompt suggestion passed to the Navie webview.
     */
    static final Key<NaviePromptSuggestion> KEY_PROMPT_SUGGESTION = Key.create("appland.navie.promptSuggestions");
    /**
     * Optional Navie thread ID to open.
     */
    static final Key<String> KEY_THREAD_ID = Key.create("appland.navie.threadId");
    /**
     * DataContext key for thread ID.
     */
    static final DataKey<String> DATA_KEY_THREAD_ID = DataKey.create("appland.navie.threadId");

    /**
     * Optional AppMap context in the context passed to @link{{@link #openEditor(Project, DataContext)}}.
     */
    static final DataKey<VirtualFile> DATA_KEY_APPMAP = DataKey.create("appland.navie.appmap");
    /**
     * Optional prompt suggestion passed to {@link #openEditor(Project, DataContext)}
     */
    static final DataKey<NaviePromptSuggestion> DATA_KEY_PROMPT_SUGGESTION = DataKey.create("appland.navie.promptSuggestions");

    public NavieEditorProvider() {
        super(TYPE_ID);
    }

    /**
     * Opens the Navie webview with the given AppMap as context.
     *
     * @param project Current project
     * @param appMap  AppMap to use as context
     */
    @RequiresEdt
    public static void openEditorForAppMap(@NotNull Project project, @NotNull VirtualFile appMap) {
        openEditor(project, DataContexts.createCustomContext(dataId -> {
            if (DATA_KEY_APPMAP.is(dataId)) {
                return appMap;
            }
            return null;
        }));
    }

    /**
     * Opens the Navie webview with the given prompt.
     *
     * @param project    Current project
     * @param suggestion Prompt to be shown in the new Navie editor
     */
    @RequiresEdt
    public static void openEditorWithPrompt(@NotNull Project project, @NotNull NaviePromptSuggestion suggestion) {
        openEditor(project, DataContexts.createCustomContext(dataId -> {
            if (DATA_KEY_PROMPT_SUGGESTION.is(dataId)) {
                return suggestion;
            }
            return null;
        }));
    }
    /**
     * Opens the Navie webview focusing an existing thread by ID.
     */
    @RequiresEdt
    public static void openEditorWithThreadId(@NotNull Project project, @NotNull String threadId) {
        openEditor(project, DataContexts.createCustomContext(dataId -> {
            if (DATA_KEY_THREAD_ID.is(dataId)) {
                return threadId;
            }
            return null;
        }));
    }

    /**
     * Open a new Navie webview with indexer port and selected derived from the current DataContext.
     * If no indexer port could be found for the context, an error message is displayed.
     *
     * @param project Current project
     * @param context Data context to locate matching indexer and selected text, if available.
     */
    @RequiresEdt
    public static void openEditor(@NotNull Project project, @NotNull DataContext context) {
        var provider = WebviewEditorProvider.findEditorProvider(TYPE_ID);
        assert provider != null;

        var editor = CommonDataKeys.EDITOR_EVEN_IF_INACTIVE.getData(context);
        chooseIndexerJsonRpcPort(project, editor, (indexerPort, codeSelection) -> {
            if (indexerPort == null) {
                AppMapNotifications.showNavieUnavailableNotification(project);
                return;
            }

            var file = provider.createVirtualFile(AppMapBundle.get("webview.navie.title"));
            KEY_INDEXER_RPC_PORT.set(file, indexerPort);
            KEY_CODE_SELECTION.set(file, codeSelection);
            KEY_APPMAP_CONTEXT_FILE.set(file, context.getData(DATA_KEY_APPMAP));
            KEY_PROMPT_SUGGESTION.set(file, context.getData(DATA_KEY_PROMPT_SUGGESTION));
            // threadId from DataContext
            String threadId = context.getData(DATA_KEY_THREAD_ID);
            if (threadId != null) {
                KEY_THREAD_ID.set(file, threadId);
            }

            AppMapProjectSettingsService.getState(project).setExplainWithNavieOpened(true);
            FileEditorManager.getInstance(project).openFile(file, true);

            if (AppMapNotifications.isWebviewTextInputBroken()) {
                AppMapNotifications.showWebviewTextInputBrokenMessage(project, true);
            }
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
                                                 @Nullable Editor editor,
                                                 @NotNull ShowNavieConsumer consumer) {
        var codeSelection = buildCodeSelection(project, editor);
        consumer.openNavie(AppLandJsonRpcService.getInstance(project).getServerPort(), codeSelection);
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