package appland;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class AppLandHTMLEditorProvider implements FileEditorProvider {
    public static void openEditor() {

    }

    @Override
    public boolean accept(@NotNull Project project, @NotNull VirtualFile file) {
        return file.getName().endsWith(".appmap.json");
    }

    @Override
    public @NotNull FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile file) {
        return null;
    }

    @Override
    public void disposeEditor(@NotNull FileEditor editor) {
        FileEditorProvider.super.disposeEditor(editor);
    }

    @Override
    public @NotNull FileEditorState readState(@NotNull Element sourceElement, @NotNull Project project, @NotNull VirtualFile file) {
        return FileEditorProvider.super.readState(sourceElement, project, file);
    }

    @Override
    public void writeState(@NotNull FileEditorState state, @NotNull Project project, @NotNull Element targetElement) {
        FileEditorProvider.super.writeState(state, project, targetElement);
    }

    @Override
    @NotNull
    @NonNls
    public String getEditorTypeId() {
        return null;
    }

    @Override
    public @NotNull FileEditorPolicy getPolicy() {
        return null;
    }
}
