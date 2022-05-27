package appland.problemsView;

import com.intellij.analysis.problemsView.toolWindow.ProblemsViewPanelProvider;
import com.intellij.analysis.problemsView.toolWindow.ProblemsViewState;
import com.intellij.analysis.problemsView.toolWindow.ProblemsViewTab;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

public class FindingsPanelProvider implements ProblemsViewPanelProvider {
    private final Project project;

    public FindingsPanelProvider(Project project) {
        this.project = project;
    }

    @Nullable
    @Override
    public ProblemsViewTab create() {
        return new FindingsViewTab(project, ProblemsViewState.getInstance(project));
    }
}
