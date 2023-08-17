package appland.toolwindow.runtimeAnalysis;

import appland.problemsView.FindingsManager;
import appland.problemsView.listener.ScannerFindingsListener;
import appland.problemsView.model.ImpactDomain;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.tree.LeafState;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * The root node groups the findings by "project name", using the same rules as the
 * data of the AppMap project picker.
 */
class RootNode extends Node implements Disposable {
    private final OverviewNode overviewNode = new OverviewNode(myProject, this);
    private final RuntimeAnalysisModel treeModel;

    protected RootNode(@NotNull Project project, @NotNull RuntimeAnalysisModel treeModel) {
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
        var domainFindings = FindingsManager.getInstance(myProject).getProblemsByImpactDomain();
        if (domainFindings.isEmpty()) {
            return List.of(overviewNode);
        }

        var children = new ArrayList<Node>();
        children.add(overviewNode);
        for (var domain : ImpactDomain.values()) { // iterating values to keep predefined order
            var findings = domainFindings.get(domain);
            if (findings != null && !findings.isEmpty()) {
                children.add(new ImpactDomainNode(myProject, this, domain, findings));
            }
        }
        return children;
    }

    @Override
    protected void update(@NotNull PresentationData presentation) {
    }

    @Override
    public void dispose() {
    }

    private void structureChanged() {
        treeModel.structureChanged(null);
    }
}
