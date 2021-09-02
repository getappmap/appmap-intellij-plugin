package appland.milestones;

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

public class UserMilestonesEditorProvider implements FileEditorProvider, DumbAware {
    private static final Key<MilestonesViewType> MILESTONE_TYPE_KEY = Key.create("appland.appmapsFile");

    public static void open(@NotNull Project project, @NotNull MilestonesViewType viewType) {
        var dummyFile = new LightVirtualFile(viewType.getPageTitle());
        MILESTONE_TYPE_KEY.set(dummyFile, viewType);
        FileEditorManager.getInstance(project).openFile(dummyFile, true);
    }

    @NotNull
    public static Boolean isQuickstartFile(@NotNull VirtualFile file) {
        return MILESTONE_TYPE_KEY.isIn(file);
    }

    @Override
    public boolean accept(@NotNull Project project, @NotNull VirtualFile file) {
        return JBCefApp.isSupported() && isQuickstartFile(file);
    }

    @Override
    public @NotNull FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile file) {
        return new UserMilestonesEditor(project, file, MILESTONE_TYPE_KEY.getRequired(file));
    }

    @Override
    public @NotNull @NonNls String getEditorTypeId() {
        return "appland.milestones";
    }

    @Override
    public @NotNull FileEditorPolicy getPolicy() {
        return FileEditorPolicy.HIDE_DEFAULT_EDITOR;
    }
}
