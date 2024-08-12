package appland.webviews;

import com.google.common.base.Predicates;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.ui.jcef.JBCefApp;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.function.Predicate;

/**
 * Base class for editor providers based on a JCEF webview.
 */
public abstract class WebviewEditorProvider implements FileEditorProvider, DumbAware {
    // key attached to the underlying VirtualFile to identify webview editors
    public static final Key<String> WEBVIEW_EDITOR_KEY = Key.create("appland.webview");

    private final @NotNull String webviewTypeId;

    public WebviewEditorProvider(@NotNull String webviewTypeId) {
        this.webviewTypeId = webviewTypeId;
    }

    /**
     * @return {@code true} if JCEF webviews are supported in the current environment.
     */
    public static boolean isSupported() {
        return JBCefApp.isSupported();
    }

    /**
     * @param webviewTypeId ID of the provider to locate
     * @return The {@code WebviewEditorProvider} with the given id, if registered as an {@code FileEditorProvider} extension.
     */
    public static @Nullable WebviewEditorProvider findEditorProvider(@NotNull String webviewTypeId) {
        return (WebviewEditorProvider) FileEditorProvider.EP_FILE_EDITOR_PROVIDER.findFirstSafe(p -> {
            return p instanceof WebviewEditorProvider && webviewTypeId.equals(((WebviewEditorProvider) p).webviewTypeId);
        });
    }

    @Override
    public abstract @NotNull FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile file);

    public abstract @Nullable Icon getEditorIcon();

    @Override
    public boolean accept(@NotNull Project project, @NotNull VirtualFile file) {
        return JBCefApp.isSupported() && isWebViewFile(file);
    }

    @Override
    public @NotNull @NonNls String getEditorTypeId() {
        return webviewTypeId;
    }

    @Override
    public @NotNull FileEditorPolicy getPolicy() {
        return FileEditorPolicy.HIDE_DEFAULT_EDITOR;
    }

    /**
     * Open a new webview editor. It reuses an already visible webview editor of this provider's type.
     *
     * @param project Current project
     * @param title   Title to show in the editor tab
     */
    public void open(@NotNull Project project, @NotNull String title) {
        open(project, title, true);
    }

    /**
     * Open a new webview editor. It reuses an already visible webview editor of this provider's type.
     *
     * @param project         Current project
     * @param title           Title to show in the editor tab
     * @param reuseOpenEditor If {@code true}, then an already available webview editor is focused instead of creating a new one.
     */
    public void open(@NotNull Project project, @NotNull String title, boolean reuseOpenEditor) {
        if (reuseOpenEditor && focusOpenEditor(project, Predicates.alwaysTrue()) != null) {
            return;
        }

        openNewFile(project, title);
    }

    public void openNewFile(@NotNull Project project, @NotNull String title) {
        FileEditorManager.getInstance(project).openFile(createVirtualFile(title), true);
    }

    public boolean isWebViewFile(@NotNull VirtualFile file) {
        return webviewTypeId.equals(WEBVIEW_EDITOR_KEY.get(file));
    }

    @NotNull
    public LightVirtualFile createVirtualFile(@NotNull String title) {
        var file = new LightVirtualFile(title);
        WEBVIEW_EDITOR_KEY.set(file, webviewTypeId);
        return file;
    }

    public WebviewEditor<?> focusOpenEditor(@NotNull Project project, @NotNull Predicate<WebviewEditor<?>> editorPredicate) {
        var openEditor = findOpenEditor(project, editorPredicate);
        if (openEditor != null) {
            FileEditorManagerEx.getInstanceEx(project).openFile(openEditor.file, true, true);
            return openEditor;
        }

        return null;
    }

    public @Nullable WebviewEditor<?> findOpenEditor(@NotNull Project project,
                                                     @NotNull Predicate<WebviewEditor<?>> editorPredicate) {
        for (var editor : FileEditorManager.getInstance(project).getAllEditors()) {
            var file = editor.getFile();
            if (file != null && isWebViewFile(file)) {
                assert editor instanceof WebviewEditor;

                if (editorPredicate.test((WebviewEditor<?>) editor)) {
                    return (WebviewEditor<?>) editor;
                }
            }
        }

        return null;
    }
}
