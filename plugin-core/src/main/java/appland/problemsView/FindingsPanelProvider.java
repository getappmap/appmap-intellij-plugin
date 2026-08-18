package appland.problemsView;

import appland.settings.AppMapApplicationSettingsService;
import com.intellij.analysis.problemsView.toolWindow.ProblemsViewPanelProvider;
import com.intellij.analysis.problemsView.toolWindow.ProblemsViewState;
import com.intellij.analysis.problemsView.toolWindow.ProblemsViewTab;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

/**
 * Adds the new AppMap panel to the main window's problems view tool window.
 * It's only added if the user is signed in and if findings are enabled in settings.
 */
public class FindingsPanelProvider implements ProblemsViewPanelProvider {
    private final Project project;

    public FindingsPanelProvider(Project project) {
        this.project = project;
    }

    @Nullable
    @Override
    public ProblemsViewTab create() {
        var settings = AppMapApplicationSettingsService.getInstance();

        // A quiescent scanner produces no new findings, but a stale appmap-findings.json would still populate
        // this tab. It's the one findings surface which isn't behind the tool window's sign-in panel.
        return settings.hasAppMapKey() && settings.isScannerEnabled()
                ? new FindingsViewTab(project, ProblemsViewState.getInstance(project))
                : null;
    }
}
