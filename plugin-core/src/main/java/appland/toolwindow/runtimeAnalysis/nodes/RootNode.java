package appland.toolwindow.runtimeAnalysis.nodes;

import appland.problemsView.FindingsManager;
import appland.problemsView.listener.ScannerFindingsListener;
import appland.problemsView.model.ScannerFinding;
import appland.toolwindow.runtimeAnalysis.RuntimeAnalysisModel;
import appland.utils.AppMapProjectUtil;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.tree.LeafState;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The root node groups the findings by "project name", using the same rules as the
 * data of the AppMap project picker.
 */
public final class RootNode extends Node implements Disposable {
    private final FindingsTableNode findingsTableNode = new FindingsTableNode(myProject, this);
    private final RuntimeAnalysisModel treeModel;

    public RootNode(@NotNull Project project,
                    @NotNull RuntimeAnalysisModel treeModel) {
        super(project, null);
        this.treeModel = treeModel;
        Disposer.register(treeModel, this);

        var busConnection = project.getMessageBus().connect(this);
        busConnection.subscribe(ScannerFindingsListener.TOPIC, new ScannerFindingsListener() {
            @Override
            public void afterFindingsChanged() {
                structureChanged();
            }
        });
    }

    @Override
    public @NotNull LeafState getLeafState() {
        return LeafState.NEVER;
    }

    @Override
    public List<? extends Node> getChildren() {
        var findings = FindingsManager.getInstance(myProject).getAllFindings();
        if (findings.isEmpty()) {
            return List.of(findingsTableNode);
        }

        var byProjectName = findings.stream().collect(Collectors.groupingBy(this::getAppMapProjectName));

        var nodes = byProjectName.entrySet().stream()
                .map(entry -> (Node) new AppMapProjectFindingsNode(myProject, this, entry.getKey(), entry.getValue()))
                .collect(Collectors.toCollection(LinkedList::new));
        // prepend the "Findings Table" node
        nodes.addFirst(findingsTableNode);

        return nodes;
    }

    private @NotNull String getAppMapProjectName(@NotNull ScannerFinding finding) {
        return ReadAction.compute(() -> AppMapProjectUtil.getAppMapProjectName(myProject, finding.getFindingsFile()));
    }

    @Override
    protected void update(@NotNull PresentationData presentation) {
        // not visible in UI, but used by tests
        presentation.setPresentableText("Root");
    }

    @Override
    public void dispose() {
    }

    private void structureChanged() {
        treeModel.structureChanged(null);
    }
}
