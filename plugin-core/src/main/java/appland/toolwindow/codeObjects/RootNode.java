package appland.toolwindow.codeObjects;

import appland.AppMapBundle;
import appland.index.ClassMapItemType;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.tree.LeafState;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

class RootNode extends Node implements Disposable {
    private final CodeObjectsModel treeModel;

    RootNode(@NotNull Project project, @NotNull CodeObjectsModel treeModel) {
        super(project, null);
        this.treeModel = treeModel;
        Disposer.register(treeModel, this);
    }

    @Override
    protected @NotNull String getDisplayName() {
        return "";
    }

    @Override
    public int getWeight() {
        return 0;
    }

    @Override
    public @NotNull LeafState getLeafState() {
        return LeafState.NEVER;
    }

    @Override
    public List<? extends Node> getChildren() {
        return List.of(
                new CodeObjectTopLevelNode(this, ClassMapItemType.Package, ClassMapItemType.ROOT.getName(), AppMapBundle.get("codeObjects.codeTopLevel.label"), Set.of(ClassMapItemType.Package, ClassMapItemType.Function)),
                new CodeObjectTopLevelNode(this, ClassMapItemType.HTTP, "", AppMapBundle.get("codeObjects.requestTopLevel.label"), Set.of(ClassMapItemType.Route)),
                new CodeObjectTopLevelNode(this, ClassMapItemType.Database, "", AppMapBundle.get("codeObjects.queryTopLevel.label"), Set.of(ClassMapItemType.Query)));
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
