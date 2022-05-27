package appland.problemsView;

import com.intellij.analysis.problemsView.toolWindow.ProblemsViewPanel;
import com.intellij.analysis.problemsView.toolWindow.ProblemsViewState;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class ScannerProblemsViewTab extends ProblemsViewPanel {
    public ScannerProblemsViewTab(@NotNull Project project, @NotNull ProblemsViewState state) {
        super(project, "appmap.scannerView", state, () -> "AppMap Scanner Findings");

        getTreeModel().setRoot(new ScannerProblemsRootNode(this));

        DumbService.getInstance(project).runWhenSmart(() -> {
            ScannerProblemsManager.getInstance(project).loadAllFindingFiles();
        });
    }
}
