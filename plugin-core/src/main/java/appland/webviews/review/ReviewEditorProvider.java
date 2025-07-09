package appland.webviews.review;

import appland.AppMapBundle;
import appland.webviews.WebviewEditorProvider;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import com.intellij.testFramework.LightVirtualFile;

import javax.swing.*;

public class ReviewEditorProvider extends WebviewEditorProvider {
    public static final String EDITOR_TYPE_ID = "appmap.review";
    static final Key<String> KEY_BASE_REF = Key.create("appmap.review.baseRef");

    public ReviewEditorProvider() {
        super(EDITOR_TYPE_ID);
    }

    @Override
    public Icon getEditorIcon() {
        return AllIcons.Actions.Preview;
    }

    @Override
    public @NotNull FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile file) {
        return new ReviewEditor(project, file);
    }

    public static void openEditor(@NotNull Project project, @NotNull String baseRef) {
        var provider = findEditorProvider(EDITOR_TYPE_ID);
        var file = provider.createVirtualFile(AppMapBundle.get("webview.review.title"));
        KEY_BASE_REF.set(file, baseRef);

        FileEditorManager.getInstance(project).openFile(file, true);
    }
}