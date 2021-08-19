package appland.milestones;

import appland.AppMapBundle;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.KeyWithDefaultValue;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.ui.jcef.JBCefApp;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class UserMilestonesEditorProvider implements FileEditorProvider, DumbAware {
    private static final Key<Boolean> QUICKSTART_KEY = KeyWithDefaultValue.create("appland.milestonesFile", false);
    private static final Key<Boolean> APPMAPS_KEY = KeyWithDefaultValue.create("appland.appmapsFile", false);

    public static void openUserQuickstart(@NotNull Project project) {
        var dummyFile = new LightVirtualFile(AppMapBundle.get("userMilestones.quickstartTitle"));
        QUICKSTART_KEY.set(dummyFile, true);
        FileEditorManager.getInstance(project).openFile(dummyFile, true);
    }

    public static void openUserAppMaps(@NotNull Project project) {
        var dummyFile = new LightVirtualFile(AppMapBundle.get("userMilestones.appmapsTitle"));
        APPMAPS_KEY.set(dummyFile, true);
        FileEditorManager.getInstance(project).openFile(dummyFile, true);
    }

    @NotNull
    public static Boolean isQuickstartFile(@NotNull VirtualFile file) {
        return QUICKSTART_KEY.getRequired(file) || APPMAPS_KEY.getRequired(file);
    }

    @Override
    public boolean accept(@NotNull Project project, @NotNull VirtualFile file) {
        return JBCefApp.isSupported() && isQuickstartFile(file);
    }

    @Override
    public @NotNull FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile file) {
        MilestonesViewType type;
        if (QUICKSTART_KEY.getRequired(file)) {
            type = MilestonesViewType.Quickstart;
        } else {
            type = MilestonesViewType.AppMapsTable;
        }
        return new UserMilestonesEditor(project, file, type);
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
