package appland.toolwindow.codeObjects;

import appland.AppMapBundle;
import appland.Icons;
import appland.index.AppMapMetadata;
import appland.index.AppMapMetadataIndex;
import appland.index.ClassMapItemType;
import appland.index.ClassMapTypeIndex;
import appland.webviews.appMap.AppMapFileEditorState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

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
    public boolean canNavigate() {
        return true;
    }

    @Override
    public boolean canNavigateToSource() {
        return true;
    }

    @Override
    public void navigate(boolean requestFocus) {
        choseAndNavigateAppMap(requestFocus);
    }

    @SuppressWarnings("DialogTitleCapitalization")
    private void choseAndNavigateAppMap(boolean requestFocus) {
        var filesToMetadata = ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
            return ReadAction.compute(() -> {
                var files = ClassMapTypeIndex.findContainingAppMapFiles(project, type, nodeId);
                return files.stream()
                        .map(file -> Pair.create(file, AppMapMetadataIndex.findAppMap(project, file)))
                        .sorted(Comparator.comparing(pair -> pair.second != null ? pair.second.getName() : ""))
                        .collect(Collectors.toList());
            });
        }, AppMapBundle.get("codeObjects.navigation.locatingFiles"), false, project);

        switch (filesToMetadata.size()) {
            case 0:
                return;
            case 1:
                openCodeObjectEditor(project, requestFocus, filesToMetadata.get(0));
                return;
            default:
                JBPopupFactory.getInstance()
                        .createListPopup(new VirtualFilePopupStep(project, filesToMetadata, requestFocus))
                        .showInFocusCenter();
        }
    }

    private void openCodeObjectEditor(@NotNull Project project,
                                      boolean requestFocus,
                                      @NotNull Pair<VirtualFile, @Nullable AppMapMetadata> selectedValue) {
        var editors = FileEditorManager.getInstance(project).openFile(selectedValue.first, requestFocus);
        if (editors.length == 1) {
            editors[0].setState(AppMapFileEditorState.createCodeObjectState(nodeId));
        }
    }

    private class VirtualFilePopupStep extends BaseListPopupStep<Pair<VirtualFile, @Nullable AppMapMetadata>> {
        private final @NotNull Project project;
        private final boolean requestFocus;

        public VirtualFilePopupStep(@NotNull Project project,
                                    @NotNull List<Pair<VirtualFile, AppMapMetadata>> files,
                                    boolean requestFocus) {
            super(AppMapBundle.get("codeObjects.chooseAppMap"), files);
            this.project = project;
            this.requestFocus = requestFocus;
        }

        @Override
        public Icon getIconFor(@NotNull Pair<VirtualFile, @Nullable AppMapMetadata> value) {
            return Icons.APPMAP_FILE;
        }

        @Override
        public boolean isSpeedSearchEnabled() {
            return true;
        }

        @Override
        public @NotNull String getTextFor(Pair<VirtualFile, @Nullable AppMapMetadata> value) {
            var file = value.first;
            var metadata = value.second;
            if (metadata != null) {
                return metadata.getName();
            }

            // fall back to relative path if metadata is unavailable
            var projectDir = ProjectUtil.guessProjectDir(project);
            var relativePath = projectDir != null ? VfsUtil.getRelativePath(file, projectDir) : null;
            return relativePath != null ? relativePath : file.getPath();
        }

        @Override
        public @Nullable PopupStep<?> onChosen(@NotNull Pair<VirtualFile, @Nullable AppMapMetadata> selectedValue, boolean finalChoice) {
            openCodeObjectEditor(ClassMapItemNavigatable.this.project, requestFocus, selectedValue);
            return null;
        }
    }
}
