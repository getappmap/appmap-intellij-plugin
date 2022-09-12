package appland.toolwindow;

import appland.AppMapBundle;
import appland.actions.StartAppMapRecordingAction;
import appland.actions.StopAppMapRecordingAction;
import appland.index.AppMapMetadata;
import appland.index.IndexedFileListenerUtil;
import appland.installGuide.InstallGuideEditorProvider;
import appland.installGuide.InstallGuideViewPage;
import appland.toolwindow.installGuide.InstallGuidePanel;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiManager;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.SearchTextField;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.content.Content;
import com.intellij.ui.tree.AsyncTreeModel;
import com.intellij.ui.treeStructure.SimpleTree;
import com.intellij.util.Alarm;
import com.intellij.util.EditSourceOnDoubleClickHandler;
import com.intellij.util.EditSourceOnEnterKeyHandler;
import com.intellij.util.SingleAlarm;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;

import static com.intellij.psi.NavigatablePsiElement.EMPTY_NAVIGATABLE_ELEMENT_ARRAY;

public class AppMapWindowPanel extends SimpleToolWindowPanel implements DataProvider, Disposable {
    private static final Logger LOG = Logger.getInstance("#appmap.toolwindow");
    private static final int TREE_REFRESH_DELAY_MILLIS = 250;
    private static final long INPUT_FILTER_DELAY_MILLIS = 250L; // + TREE_REFRESH_DELAY after typing stopped

    @NotNull
    private final SimpleTree tree;
    @NotNull
    private final Project project;
    // debounce requests for AppMap tree refresh
    private final SingleAlarm treeRefreshAlarm;
    // debounce filter requests when search text changes
    private volatile Alarm filterInputAlarm;

    private volatile boolean isToolWindowVisible;
    private volatile boolean hasPendingTreeRefresh;

    public AppMapWindowPanel(@NotNull Project project, @NotNull Content parent) {
        super(true);
        Disposer.register(parent, this);

        var appMapModel = new AppMapModel(project);
        this.project = project;
        this.tree = createTree(this, appMapModel);
        this.treeRefreshAlarm = new SingleAlarm(appMapModel::refresh, TREE_REFRESH_DELAY_MILLIS, this, Alarm.ThreadToUse.POOLED_THREAD);

        setToolbar(createToolBar(appMapModel));
        setContent(ScrollPaneFactory.createScrollPane(tree));
        add(createUserMilestonesPanel(), BorderLayout.SOUTH);

        IndexedFileListenerUtil.registerListeners(project, this, true, false, () -> rebuild(false));
    }

    @Override
    public void dispose() {
        LOG.debug("disposing AppMap tool window");
    }

    /**
     * Creates a panel with the search text field and the start and stop actions.
     */
    @NotNull
    private JComponent createToolBar(AppMapModel appMapModel) {
        var actions = new DefaultActionGroup();
        actions.add(new StartAppMapRecordingAction());
        actions.add(new StopAppMapRecordingAction());

        var bar = ActionManager.getInstance().createActionToolbar("appmapToolWindow", actions, true);
        bar.setTargetComponent(this);
        bar.setLayoutPolicy(ActionToolbar.NOWRAP_LAYOUT_POLICY);

        var panel = new JPanel(new BorderLayout());
        panel.add(createNameFilter(appMapModel), BorderLayout.CENTER);
        panel.add(bar.getComponent(), BorderLayout.EAST);
        return panel;
    }

    @NotNull
    private SimpleTree createTree(@NotNull Disposable disposable, @NotNull TreeModel treeModel) {
        var tree = new SimpleTree(new AsyncTreeModel(treeModel, disposable));
        tree.setCellRenderer(new AppMapModel.TreeCellRenderer());
        tree.getEmptyText().setText(AppMapBundle.get("toolwindow.appmap.emptyText"));
        tree.getEmptyText().appendSecondaryText(
                AppMapBundle.get("toolwindow.appmap.installAgentEmptyText"),
                SimpleTextAttributes.LINK_ATTRIBUTES,
                e -> InstallGuideEditorProvider.open(project, InstallGuideViewPage.InstallAgent));
        tree.setRootVisible(false);
        tree.setShowsRootHandles(false);

        TreeUtil.installActions(tree);
        new EditSourceOnDoubleClickHandler.TreeMouseListener(tree, null).installOn(tree);
        EditSourceOnEnterKeyHandler.install(tree);
        return tree;
    }

    @NotNull
    private JPanel createUserMilestonesPanel() {
        return new InstallGuidePanel(project);
    }

    @Override
    public @Nullable Object getData(@NotNull @NonNls String dataId) {
        if (CommonDataKeys.NAVIGATABLE.is(dataId)) {
            var file = getSelectedFile();
            return file == null ? null : PsiManager.getInstance(project).findFile(file);
        }

        if (CommonDataKeys.NAVIGATABLE_ARRAY.is(dataId)) {
            var file = getSelectedFile();
            if (file == null) {
                return null;
            }
            var psiFile = PsiManager.getInstance(project).findFile(file);
            return psiFile == null ? EMPTY_NAVIGATABLE_ELEMENT_ARRAY : new Navigatable[]{psiFile};
        }

        if (CommonDataKeys.VIRTUAL_FILE.is(dataId)) {
            return getSelectedFile();
        }

        if (CommonDataKeys.VIRTUAL_FILE_ARRAY.is(dataId)) {
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
        var node = path.getLastPathComponent();
        if (!(node instanceof AppMapMetadata)) {
            return null;
        }

        var filepath = ((AppMapMetadata) node).getSystemIndependentFilepath();
        return LocalFileSystem.getInstance().findFileByPath(filepath);
    }

    @NotNull
    private SearchTextField createNameFilter(@NotNull AppMapModel appMapModel) {
        var textFilter = new SearchTextField();
        filterInputAlarm = new Alarm(textFilter, this);

        textFilter.getTextEditor().getEmptyText().setText(AppMapBundle.get("toolwindow.appmap.filterEmptyText"));
        textFilter.getTextEditor().addActionListener(e -> {
            LOG.debug("applying appmap filter: " + textFilter.getText());
            appMapModel.setNameFilter(textFilter.getText());
            rebuild(false);
        });
        textFilter.addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                LOG.debug("applying delayed appmap filter: " + textFilter.getText());
                filterInputAlarm.cancelAllRequests();
                filterInputAlarm.addComponentRequest(() -> {
                    appMapModel.setNameFilter(textFilter.getText());
                    rebuild(false);
                }, INPUT_FILTER_DELAY_MILLIS);
            }
        });
        return textFilter;
    }

    public void onToolWindowShown() {
        LOG.debug("onToolWindowShown");
        this.isToolWindowVisible = true;

        if (hasPendingTreeRefresh) {
            LOG.debug("Triggering pending refresh of AppMap tool window");
            rebuild(true);
        }
    }

    public void onToolWindowHidden() {
        LOG.debug("onToolWindowHidden");
        this.isToolWindowVisible = false;
    }

    private void rebuild(boolean force) {
        if (force || isToolWindowVisible) {
            hasPendingTreeRefresh = false;
            treeRefreshAlarm.cancelAndRequest();
        } else {
            LOG.debug("rebuild with hidden AppMap tool window");
            hasPendingTreeRefresh = true;
        }
    }
}
