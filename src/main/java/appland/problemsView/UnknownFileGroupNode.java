package appland.problemsView;

import appland.AppMapBundle;
import appland.Icons;
import com.intellij.analysis.problemsView.toolWindow.Node;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.openapi.project.Project;
import com.intellij.ui.tree.LeafState;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.stream.Collectors;

import static com.intellij.ui.SimpleTextAttributes.GRAYED_ATTRIBUTES;
import static com.intellij.ui.SimpleTextAttributes.REGULAR_ATTRIBUTES;

class UnknownFileGroupNode extends Node {
    UnknownFileGroupNode(@NotNull Node parent) {
        super(parent);
    }

    @NotNull
    @Override
    public String getName() {
        return AppMapBundle.get("problemsView.unknownFilesGroup.title");
    }

    @Override
    public @NotNull LeafState getLeafState() {
        return LeafState.NEVER;
    }

    @Override
    protected void update(@NotNull Project project, @NotNull PresentationData presentation) {
        presentation.setIcon(Icons.APPMAP_FILE_SMALL);
        presentation.addText(getName(), REGULAR_ATTRIBUTES);

        var count = FindingsManager.getInstance(getProject()).getOtherProblemCount();
        presentation.addText(" " + AppMapBundle.get("problemsView.unknownFilesGroup.fileCountSuffix", count), GRAYED_ATTRIBUTES);
    }

    @NotNull
    @Override
    public Collection<Node> getChildren() {
        var unknownFileProblems = FindingsManager.getInstance(getProject()).getOtherProblems();

        return unknownFileProblems.stream()
                .map(problem -> new UnknownFileNode(this, problem))
                .collect(Collectors.toList());
    }
}
