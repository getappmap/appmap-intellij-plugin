package appland.projectPicker.editor;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.ui.jcef.JBCefApp;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class ProjectPickerEditorProvider implements FileEditorProvider, DumbAware {
    private static final Key<Boolean> TYPE_KEY = Key.create("appland.projectPicker");

    public static void open(@NotNull Project project) {
        var dummyFile = new LightVirtualFile("Project Picker");
        TYPE_KEY.set(dummyFile, true);
        FileEditorManager.getInstance(project).openFile(dummyFile, true);
    }

    @NotNull
    public static Boolean isSupportedFile(@NotNull VirtualFile file) {
        return TYPE_KEY.isIn(file);
    }

    @Override
    public boolean accept(@NotNull Project project, @NotNull VirtualFile file) {
        return JBCefApp.isSupported() && isSupportedFile(file);
    }

    @Override
    public @NotNull FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile file) {
        return new ProjectPickerEditor(project, file);
    }

    @Override
    public @NotNull @NonNls String getEditorTypeId() {
        return "appland.projectPicker";
    }

    @Override
    public @NotNull FileEditorPolicy getPolicy() {
        return FileEditorPolicy.HIDE_DEFAULT_EDITOR;
    }
}
