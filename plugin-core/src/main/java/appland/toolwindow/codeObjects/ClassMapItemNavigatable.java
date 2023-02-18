package appland.toolwindow.codeObjects;

import appland.AppMapBundle;
import appland.Icons;
import appland.index.ClassMapItemType;
import appland.index.ClassMapTypeIndex;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class ClassMapItemNavigatable implements Navigatable {
    private final Project project;
    private final ClassMapItemType type;
    private final String nodeId;

    public ClassMapItemNavigatable(@NotNull Project project, @NotNull ClassMapItemType type, @NotNull String nodeId) {
        this.project = project;
        this.type = type;
        this.nodeId = nodeId;
    }

    @Override
    public void navigate(boolean requestFocus) {
        //noinspection DialogTitleCapitalization
        var files = ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
            return ReadAction.compute(() -> new ArrayList<>(ClassMapTypeIndex.findContainingAppMapFiles(project, type, nodeId)));
        }, AppMapBundle.get("codeObjects.navigation.locatingFiles"), false, project);

        switch (files.size()) {
            case 0:
                return;
            case 1:
                FileEditorManager.getInstance(project).openFile(files.get(0), requestFocus);
                return;
            default:
                JBPopupFactory.getInstance()
                        .createListPopup(new VirtualFilePopupStep(project, files))
                        .showInFocusCenter();
        }
    }

    @Override
    public boolean canNavigate() {
        return true;
    }

    @Override
    public boolean canNavigateToSource() {
        return true;
    }

    private static class VirtualFilePopupStep extends BaseListPopupStep<VirtualFile> {
        private final Project project;

        public VirtualFilePopupStep(@NotNull Project project, @NotNull List<VirtualFile> files) {
            super(AppMapBundle.get("codeObjects.chooseAppMap"), files);
            this.project = project;
        }

        @Override
        public Icon getIconFor(VirtualFile value) {
            return Icons.APPMAP_FILE;
        }

        @Override
        public boolean isSpeedSearchEnabled() {
            return true;
        }

        @Override
        public @NotNull String getTextFor(VirtualFile value) {
            var projectDir = ProjectUtil.guessProjectDir(project);
            var relativePath = projectDir != null ? VfsUtil.getRelativePath(value, projectDir) : null;
            return relativePath != null ? relativePath : value.getPath();
        }

        @Override
        public @Nullable PopupStep<?> onChosen(VirtualFile selectedValue, boolean finalChoice) {
            FileEditorManager.getInstance(project).openFile(selectedValue, true);
            return null;
        }
    }
}
