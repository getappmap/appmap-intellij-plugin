package appland.toolwindow;

import appland.Messages;
import appland.files.AppMapFiles;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.AsyncFileListener;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiManager;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.SearchTextField;
import com.intellij.ui.content.Content;
import com.intellij.ui.tree.AsyncTreeModel;
import com.intellij.ui.tree.StructureTreeModel;
import com.intellij.ui.treeStructure.SimpleTree;
import com.intellij.util.EditSourceOnDoubleClickHandler;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.util.Comparator;

import static com.intellij.psi.NavigatablePsiElement.EMPTY_NAVIGATABLE_ELEMENT_ARRAY;

public class AppMapWindowPanel extends SimpleToolWindowPanel implements DataProvider, Disposable {
    private static final Logger LOG = Logger.getInstance("#appmap.toolwindow");

    @NotNull
    private final SimpleTree tree;
    private final StructureTreeModel<AppMapTreeModel> treeModel;
    @NotNull
    private final Project project;

    public AppMapWindowPanel(@NotNull Project project, @NotNull Content parent) {
        super(true);
        Disposer.register(parent, this);

        var appMapModel = new AppMapTreeModel(project);

        this.project = project;
        this.treeModel = createModel(appMapModel, this);
        this.tree = createTree(this, treeModel);

        setToolbar(createNameFilter(appMapModel));
        setContent(ScrollPaneFactory.createScrollPane(tree));

        // refresh when dumb mode changes
        project.getMessageBus().connect(this).subscribe(DumbService.DUMB_MODE, new DumbService.DumbModeListener() {
            @Override
            public void enteredDumbMode() {
                rebuild();
            }

            @Override
            public void exitDumbMode() {
                rebuild();
            }
        });

        // refresh when VirtualFiles change
        VirtualFileManager.getInstance().addAsyncFileListener(events -> {
            var appMapChanged = false;
            for (var event : events) {
                var file = event.getFile();
                if (file != null && AppMapFiles.isAppMap(file)) {
                    LOG.debug("appmap VirtualFile changes, rebuilding tree");
                    appMapChanged = true;
                    break;
                }
            }

            return !appMapChanged ? null : new AsyncFileListener.ChangeApplier() {
                @Override
                public void afterVfsChange() {
                    LOG.debug("afterVfsChange, invalidating tree");
                    rebuild();
                }
            };
        }, this);
    }

    @Override
    public void dispose() {
    }

    @NotNull
    private StructureTreeModel<AppMapTreeModel> createModel(@NotNull AppMapTreeModel model, @NotNull Disposable disposable) {
        var treeModel = new StructureTreeModel<>(model, disposable);
        // sort alphabetically, case-insensitive
        treeModel.setComparator(Comparator.comparing(NodeDescriptor::toString, String.CASE_INSENSITIVE_ORDER));
        return treeModel;
    }

    @NotNull
    private SimpleTree createTree(@NotNull Disposable disposable, StructureTreeModel<AppMapTreeModel> treeModel) {
        var tree = new SimpleTree(new AsyncTreeModel(treeModel, true, disposable));
        tree.getEmptyText().setText(Messages.get("toolwindow.appmap.emptyText"));
        tree.setRootVisible(false);
        tree.setShowsRootHandles(false);

        TreeUtil.installActions(tree);
        new EditSourceOnDoubleClickHandler.TreeMouseListener(tree, null).installOn(tree);
        return tree;
    }

    public void rebuild() {
        treeModel.invalidate();
    }

    @Override
    public @Nullable Object getData(@NotNull @NonNls String dataId) {
        if (CommonDataKeys.NAVIGATABLE.is(dataId)) {
            var file = getSelectedFile();
            return file == null ? null : PsiManager.getInstance(project).findFile(file);
        } else if (CommonDataKeys.NAVIGATABLE_ARRAY.is(dataId)) {
            var file = getSelectedFile();
            if (file == null) {
                return null;
            }
            var psiFile = PsiManager.getInstance(project).findFile(file);
            return psiFile == null ? EMPTY_NAVIGATABLE_ELEMENT_ARRAY : new Navigatable[]{psiFile};
        } else if (CommonDataKeys.VIRTUAL_FILE.is(dataId)) {
            return getSelectedFile();
        } else if (CommonDataKeys.VIRTUAL_FILE_ARRAY.is(dataId)) {
            var file = getSelectedFile();
            return file == null ? VirtualFile.EMPTY_ARRAY : new VirtualFile[]{file};
        }

        return super.getData(dataId);
    }

    private @Nullable VirtualFile getSelectedFile() {
        TreePath path = tree.getSelectionPath();
        if (path == null) {
            return null;
        }
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        Object userObject = node.getUserObject();
        if (!(userObject instanceof AppMapTreeModel.SingleAppMapDescriptor)) {
            return null;
        }
        var data = ((AppMapTreeModel.SingleAppMapDescriptor) userObject).getAppMapData();
        var filepath = data.getSystemIndependentFilepath();
        return LocalFileSystem.getInstance().findFileByPath(filepath);
    }

    @NotNull
    private SearchTextField createNameFilter(@NotNull AppMapTreeModel appMapModel) {
        var textFilter = new SearchTextField();
        textFilter.getTextEditor().getEmptyText().setText(Messages.get("toolwindow.appmap.filterEmptyText"));
        textFilter.getTextEditor().addActionListener(e -> {
            LOG.debug("applying appmap filter: " + textFilter.getText());
            appMapModel.setNameFilter(textFilter.getText());
            rebuild();
        });
        return textFilter;
    }
}
