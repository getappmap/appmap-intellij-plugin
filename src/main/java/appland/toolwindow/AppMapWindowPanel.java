package appland.toolwindow;

import appland.Messages;
import appland.files.AppMapFiles;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.AsyncFileListener;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.ui.ScrollPaneFactory;
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

public class AppMapWindowPanel extends SimpleToolWindowPanel implements DataProvider, Disposable {
    private static final Logger LOG = Logger.getInstance("#appmap.toolwindow");

    @NotNull
    private final SimpleTree tree;
    private final StructureTreeModel<AppMapTreeModel> treeModel;
    @NotNull
    private final Project project;

    public AppMapWindowPanel(@NotNull Project project, @NotNull Content parent) {
        super(true);
        this.project = project;
        this.treeModel = createModel(project, this);
        this.tree = createTree(this, treeModel);

        Disposer.register(parent, this);
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
    private StructureTreeModel<AppMapTreeModel> createModel(@NotNull Project project, @NotNull Disposable disposable) {
        var model = new AppMapTreeModel(project);
        var treeModel = new StructureTreeModel<>(model, disposable);
        // sort alphabetically
        treeModel.setComparator(Comparator.comparing(NodeDescriptor::toString));
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
            if (file != null) {
                openFile(file);
            }
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

    private void openFile(@NotNull VirtualFile file) {
        ApplicationManager.getApplication().invokeLater(() -> {
            FileEditorManager.getInstance(project).openFile(file, true, true);
        }, ModalityState.defaultModalityState(), project.getDisposed());
    }
}
