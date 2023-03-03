package appland.toolwindow.codeObjects;

import appland.AppMapBundle;
import appland.Icons;
import appland.files.FileLocation;
import appland.index.AppMapMetadata;
import appland.index.AppMapMetadataIndex;
import appland.index.ClassMapItemType;
import appland.index.ClassMapTypeIndex;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.ui.Messages;
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
    private final @Nullable String location;

    public ClassMapItemNavigatable(@NotNull Project project, @NotNull ClassMapItemType type, @NotNull String nodeId, @Nullable String location) {
        this.project = project;
        this.type = type;
        this.nodeId = nodeId;
        this.location = location;
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
        if (type == ClassMapItemType.Function) {
            // functions: navigate to source file
            navigateToSourceFile(requestFocus);
        } else {
            // all other types: open the AppMap
            choseAndNavigateAppMap(requestFocus);
        }
    }

    private void navigateToSourceFile(boolean requestFocus) {
        if (location != null) {
            var sourceFileWithLocation = ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
                return ReadAction.compute(() -> {
                    var parsedLocation = FileLocation.parse(location);
                    if (parsedLocation != null) {
                        // we're assuming that AppMaps containing the same nodeId are all valid, first is as good as any
                        var baseFile = ClassMapTypeIndex.findContainingAppMapFiles(project, type, nodeId)
                                .stream()
                                .findFirst()
                                .orElseGet(() -> ProjectUtil.guessProjectDir(project));
                        if (baseFile != null) {
                            return Pair.create(parsedLocation.resolveFilePath(project, baseFile), parsedLocation);
                        }
                    }
                    return null;
                });
            }, AppMapBundle.get("codeObjects.navigation.locatingSourceFile"), false, project);

            if (sourceFileWithLocation != null) {
                var line = sourceFileWithLocation.second.getZeroBasedLine(0);
                new OpenFileDescriptor(project, sourceFileWithLocation.first, line, -1).navigate(requestFocus);
            } else {
                Messages.showErrorDialog(AppMapBundle.get("codeObjects.navigation.locationSourceFailed.text"),
                        AppMapBundle.get("codeObjects.navigation.locationSourceFailed.title"));
            }
        }
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
                FileEditorManager.getInstance(project).openFile(filesToMetadata.get(0).first, requestFocus);
                return;
            default:
                JBPopupFactory.getInstance()
                        .createListPopup(new VirtualFilePopupStep(project, filesToMetadata, requestFocus))
                        .showInFocusCenter();
        }
    }

    private static class VirtualFilePopupStep extends BaseListPopupStep<Pair<VirtualFile, @Nullable AppMapMetadata>> {
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
            FileEditorManager.getInstance(project).openFile(selectedValue.first, requestFocus);
            return null;
        }
    }
}
