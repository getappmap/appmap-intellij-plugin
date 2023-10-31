package appland.toolwindow.appmap.nodes;

import appland.index.AppMapMetadata;
import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.tree.LeafState;
import lombok.Value;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

class ProjectNode extends Node {
    private final String projectName;
    private final List<AppMapMetadata> appMaps;

    public ProjectNode(@NotNull Project project,
                       @NotNull Node parentNode,
                       @NotNull String projectName,
                       @NotNull List<AppMapMetadata> appMaps) {
        super(project, parentNode);
        this.projectName = projectName;
        this.appMaps = appMaps;
    }

    @Override
    public @NotNull LeafState getLeafState() {
        return LeafState.NEVER;
    }

    @Override
    protected void update(@NotNull PresentationData presentation) {
        presentation.setPresentableText(projectName);
        presentation.setIcon(AllIcons.Nodes.Module);
    }

    @Override
    public List<? extends Node> getChildren() {
        var byGroupName = appMaps.stream().collect(Collectors.groupingBy(ProjectNode::createAppMapGroup));
        return byGroupName.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new AppMapGroupNode(myProject,
                        this,
                        entry.getKey().getTitle(),
                        entry.getKey().sortByModificationDate,
                        entry.getValue()))
                .collect(Collectors.toCollection(LinkedList::new));
    }

    private static @NotNull GroupMetadata createAppMapGroup(@NotNull AppMapMetadata appMap) {
        var languageName = appMap.getLanguageName();
        var recorderType = appMap.getRecorderType();
        var recorderName = appMap.getRecorderName();

        if (languageName == null || recorderType == null || recorderName == null) {
            // fallback to AppMap name
            return new GroupMetadata(appMap.getName(), false);
        }

        var capitalizedType = StringUtil.capitalize(recorderType);
        var title = String.format("%s (%s + %s)", capitalizedType, languageName, recorderName);
        return new GroupMetadata(title, isSortByDateRecorderType(recorderType));
    }

    private static boolean isSortByDateRecorderType(@NotNull String recorderType) {
        return "requests".equals(recorderType) || "remote".equals(recorderType);
    }

    @Value
    private static class GroupMetadata implements Comparable<GroupMetadata> {
        @NotNull String title;
        boolean sortByModificationDate;

        @Override
        public int compareTo(@NotNull ProjectNode.GroupMetadata other) {
            return title.compareTo(other.title);
        }
    }

    @Override
    public @NotNull List<VirtualFile> getFiles() {
        return appMaps.stream()
                .map(AppMapMetadata::getAppMapFile)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
