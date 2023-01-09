package appland.webviews.findingDetails;

import appland.AppMapBundle;
import appland.Icons;
import appland.problemsView.ScannerProblem;
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

public class FindingDetailsEditorProvider extends WebviewEditorProvider {
    private static final String TYPE_ID = "appland.findingDetails";
    static final Key<List<ScannerProblem>> KEY_FINDINGS = Key.create("appmap.findingDetailsData");

    public FindingDetailsEditorProvider() {
        super(TYPE_ID);
    }

    public static void openNewEditor(@NotNull Project project, @NotNull List<ScannerProblem> findings) {
        var provider = WebviewEditorProvider.findEditorProvider(TYPE_ID);
        assert provider != null;

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

    private static String findEditorTitle(@NotNull List<ScannerProblem> findings) {
        return findings.isEmpty()
                ? AppMapBundle.get("webview.findingDetails.title")
                : findings.get(0).getFinding().ruleTitle;
    }
}
