package appland.webviews.findingDetails;

import appland.AppMapBundle;
import appland.Icons;
import appland.problemsView.model.ScannerFinding;
import appland.webviews.WebviewEditor;
import appland.webviews.WebviewEditorProvider;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
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
    static final Key<String> KEY_FINDING_HASH = Key.create("appmap.findingDetailsHash");
    static final Key<List<ScannerFinding>> KEY_FINDINGS = Key.create("appmap.findingDetailsData");

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
    public static void openEditor(@NotNull Project project,
                                  @NotNull String findingHash,
                                  @NotNull List<ScannerFinding> findings) {
        var provider = WebviewEditorProvider.findEditorProvider(TYPE_ID);
        assert provider != null;

        // we only want to reuse for the same list of findings
        if (provider.focusOpenEditor(project, webviewEditor -> isEditorWithFindings(webviewEditor, findings))) {
            return;
        }

        var file = provider.createVirtualFile(findEditorTitle(findings));
        KEY_FINDING_HASH.set(file, findingHash);
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

    private static boolean isEditorWithFindings(@NotNull WebviewEditor<?> webview, @NotNull List<ScannerFinding> findings) {
        return Objects.equals(findings, KEY_FINDINGS.get(webview.getFile()));
    }

    private static String findEditorTitle(@NotNull List<ScannerFinding> findings) {
        return findings.isEmpty()
                ? AppMapBundle.get("webview.findingDetails.title")
                : findings.get(0).ruleTitle;
    }
}
