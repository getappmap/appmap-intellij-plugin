package appland.toolwindow;

import appland.Icons;
import appland.index.AppMapMetadata;
import appland.index.AppMapMetadataIndex;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.AbstractTreeStructure;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.ide.util.treeView.PresentableNodeDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.util.ArrayUtilRt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Tree model used by the tree in the AppMap tool window.
 * <p>
 * It supports only one level in the hierarchy. The root node is hidden. Underneath all appmap files are listed.
 */
class AppMapTreeModel extends AbstractTreeStructure {
    private final Object ROOT = new Object();
    private final Project project;
    @Nullable
    private String nameFilter;

    public AppMapTreeModel(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public @NotNull Object getRootElement() {
        return ROOT;
    }

    @Override
    public boolean isToBuildChildrenInBackground(@NotNull Object element) {
        return true;
    }

    @Override
    public @Nullable Object getParentElement(@NotNull Object element) {
        return element == ROOT ? null : ROOT;
    }

    @Override
    public Object @NotNull [] getChildElements(@NotNull Object element) {
        return element == ROOT ? AppMapMetadataIndex.findAppMaps(project, nameFilter).toArray() : ArrayUtilRt.EMPTY_OBJECT_ARRAY;
    }

    @Override
    public boolean isAlwaysLeaf(@NotNull Object element) {
        return element instanceof AppMapMetadata;
    }

    @Override
    public @NotNull NodeDescriptor<?> createDescriptor(@NotNull Object element, @Nullable NodeDescriptor parentDescriptor) {
        if (element == ROOT) {
            return new AppMapRootNodeDescriptor(project, parentDescriptor);
        }

        if (element instanceof AppMapMetadata) {
            return new SingleAppMapDescriptor(project, parentDescriptor, (AppMapMetadata) element);
        }

        throw new IllegalStateException("unexpected tree node element type: " + element);
    }

    @Override
    public void commit() {
    }

    @Override
    public boolean hasSomethingToCommit() {
        return false;
    }

    public @Nullable String getNameFilter() {
        return nameFilter;
    }

    public void setNameFilter(@Nullable String nameFilter) {
        this.nameFilter = nameFilter;
    }

    static class SingleAppMapDescriptor extends PresentableNodeDescriptor<Object> {
        private final AppMapMetadata data;

        public SingleAppMapDescriptor(@NotNull Project project, @Nullable NodeDescriptor<?> parentDescriptor, @NotNull AppMapMetadata data) {
            super(project, parentDescriptor);
            this.data = data;

            myName = data.getName();

            var presentation = getTemplatePresentation();
            presentation.setIcon(Icons.APPMAP_FILE_SMALL);
            presentation.setPresentableText(data.getName());
            presentation.setLocationString(data.getFilename());
        }

        @Override
        protected boolean shouldUpdateData() {
            return false;
        }

        @Override
        protected void update(@NotNull PresentationData presentation) {
        }

        @Override
        public String getName() {
            return data.getName();
        }

        @Override
        public Object getElement() {
            return data;
        }

        public AppMapMetadata getAppMapData() {
            return data;
        }
    }

    private class AppMapRootNodeDescriptor extends NodeDescriptor<Object> {
        public AppMapRootNodeDescriptor(@NotNull Project project, @Nullable NodeDescriptor<?> parentDescriptor) {
            super(project, parentDescriptor);
        }

        @Override
        public boolean update() {
            return false;
        }

        @Override
        public Object getElement() {
            return ROOT;
        }
    }
}
