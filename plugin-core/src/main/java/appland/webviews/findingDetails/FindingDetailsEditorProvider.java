package appland.webviews.findingDetails;

import appland.AppMapBundle;
import appland.Icons;
import appland.problemsView.ScannerProblem;
import appland.webviews.WebviewEditor;
import appland.webviews.WebviewEditorProvider;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;
import java.util.Objects;

public class FindingDetailsEditorProvider extends WebviewEditorProvider {
    private static final String TYPE_ID = "appland.findingDetails";
    static final Key<List<ScannerProblem>> KEY_FINDINGS = Key.create("appmap.findingDetailsData");

    public FindingDetailsEditorProvider() {
        super(TYPE_ID);
    }

    /**
     * Opens a new webview for the given list of findings.
     * An existing editor for the same list of findings is reused instead of creating a new editor.
     *
     * @param project  Current project
     * @param findings Findings to show in the webview
     */
    public static void openEditor(@NotNull Project project, @NotNull List<ScannerProblem> findings) {
        var provider = WebviewEditorProvider.findEditorProvider(TYPE_ID);
        assert provider != null;

        if (reuseExistingEditor(project, findings, provider)) {
            return;
        }

        var file = provider.createVirtualFile(findEditorTitle(findings));
        KEY_FINDINGS.set(file, findings);
        FileEditorManager.getInstance(project).openFile(file, true);
    }

    @Override
    public @NotNull FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile file) {
        return new FindingDetailsEditor(project, file);
    }

    @Override
    public @Nullable Icon getEditorIcon() {
        return Icons.APPMAP_FILE;
    }

    /**
     * Try to re-use an already open editor, we can't use the logic of {@link WebviewEditorProvider}
     * because we only want to reuse for the same list of findings.
     */
    private static boolean reuseExistingEditor(@NotNull Project project,
                                               @NotNull List<ScannerProblem> findings,
                                               @NotNull WebviewEditorProvider provider) {
        for (var editor : FileEditorManager.getInstance(project).getAllEditors()) {
            var file = editor.getFile();
            if (file != null && provider.isWebViewFile(file)) {
                assert editor instanceof WebviewEditor;
                if (Objects.equals(findings, KEY_FINDINGS.get(file))) {
                    FileEditorManagerEx.getInstanceEx(project).openFile(file, true, true);
                    return true;
                }
            }
        }
        return false;
    }

    private static String findEditorTitle(@NotNull List<ScannerProblem> findings) {
        return findings.isEmpty()
                ? AppMapBundle.get("webview.findingDetails.title")
                : findings.get(0).getFinding().ruleTitle;
    }
}
