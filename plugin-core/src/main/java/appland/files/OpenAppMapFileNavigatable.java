package appland.files;

import appland.problemsView.model.ScannerFinding;
import appland.webviews.appMap.AppMapFileEditor;
import appland.webviews.appMap.AppMapFileEditorState;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Navigatable to open an AppMap webview editor. If the finding is defined, then it the webview will show it.
 */
public class OpenAppMapFileNavigatable implements Navigatable {
    private final @NotNull Project project;
    private final @NotNull VirtualFile appMapFile;
    private final @Nullable ScannerFinding finding;

    public OpenAppMapFileNavigatable(@NotNull Project project,
                                     @NotNull VirtualFile appMapFile,
                                     @Nullable ScannerFinding finding) {
        this.project = project;
        this.appMapFile = appMapFile;
        this.finding = finding;
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
        var appMapEditors = FileEditorManager.getInstance(project).openFile(appMapFile, true);
        if (finding != null && appMapEditors.length == 1 && appMapEditors[0] instanceof AppMapFileEditor) {
            var editorState = AppMapFileEditorState.createViewFlowState(finding.getEventId(), finding.relatedEvents);
            ((AppMapFileEditor) appMapEditors[0]).setWebViewState(editorState);
        }
    }
}
