package appland.toolwindow.appmap;

import appland.AppMapBundle;
import appland.Icons;
import appland.actions.StartAppMapRecordingAction;
import appland.actions.StopAppMapRecordingAction;
import appland.index.IndexedFileListenerUtil;
import appland.installGuide.InstallGuideEditorProvider;
import appland.installGuide.InstallGuideViewPage;
import appland.toolwindow.AppMapContentPanel;
import appland.toolwindow.AppMapToolWindowContent;
import appland.toolwindow.CollapsiblePanel;
import appland.toolwindow.appmap.nodes.Node;
import appland.toolwindow.codeObjects.CodeObjectsPanel;
import appland.toolwindow.installGuide.InstallGuidePanel;
import appland.toolwindow.installGuide.UrlLabel;
import appland.toolwindow.runtimeAnalysis.RuntimeAnalysisPanel;
import com.intellij.ide.CommonActionsManager;
import com.intellij.ide.DefaultTreeExpander;
import com.intellij.ide.DeleteProvider;
import com.intellij.ide.TitledHandler;
import com.intellij.ide.actions.DeleteAction;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.actions.VirtualFileDeleteProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.ui.Splitter;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.NlsActions;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.ui.*;
import com.intellij.ui.components.panels.VerticalBox;
import com.intellij.ui.tree.AsyncTreeModel;
import com.intellij.ui.treeStructure.SimpleTree;
import com.intellij.util.Alarm;
import com.intellij.util.EditSourceOnDoubleClickHandler;
import com.intellij.util.EditSourceOnEnterKeyHandler;
import com.intellij.util.SingleAlarm;
import com.intellij.util.ui.JBDimension;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.tree.TreeModel;
import java.awt.*;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public class AppMapWindowPanel extends SimpleToolWindowPanel implements DataProvider, Disposable, AppMapToolWindowContent {
    private static final Logger LOG = Logger.getInstance("#appmap.toolwindow");
    private static final int TREE_REFRESH_DELAY_MILLIS = 250;
    private static final long INPUT_FILTER_DELAY_MILLIS = 250L; // + TREE_REFRESH_DELAY after typing stopped
    // key to provide all available AppMaps in our data context for the "Delete all AppMaps" action
    public static final @NotNull DataKey<VirtualFile[]> KEY_ALL_APPMAPS = DataKey.create("appland.allAppMaps");

    @NotNull
    private final SimpleTree tree;
    @NotNull
    private final Project project;
    // debounce requests for AppMap tree refresh
    private final SingleAlarm treeRefreshAlarm;
    // debounce filter requests when search text changes
    private final SearchTextField textFilter = new SearchTextField();
    private final Alarm filterInputAlarm = new Alarm(textFilter, this);
    // custom delete provider to support "Edit -> Delete" with an overridden action label
    private final DeleteProvider deleteDataProvider = new AppMapDeleteProvider();

    private volatile boolean isToolWindowVisible;
    private volatile boolean hasPendingTreeRefresh;

    // Hierarchy of components, which are expanded to make the focus component visible.
    // We're storing the path instead of iterating the parents of the tree component,
    // because CollapsiblePanel doese not keep invisible subcomponents as children.
    private @NotNull List<Component> appMapTreePanelPath;

    public AppMapWindowPanel(@NotNull Project project, @NotNull Disposable parent) {
        super(true);
        Disposer.register(parent, this);

        var appMapModel = new AppMapModel(project);
        this.project = project;
        this.tree = createTree(project, this, appMapModel);
        this.treeRefreshAlarm = new SingleAlarm(() -> refreshAndExpand(appMapModel), TREE_REFRESH_DELAY_MILLIS, this, Alarm.ThreadToUse.POOLED_THREAD);

        IndexedFileListenerUtil.registerListeners(project, this, true, false, () -> rebuild(false));

        // create the content panel
        var appMapPanel = createAppMapPanel(project, tree, appMapModel);
        var splitter = createSplitter();
        splitter.setFirstComponent(appMapPanel);
        splitter.setSecondComponent(createSouthPanel(project, this));

        var panel = new JPanel(new BorderLayout());
        panel.add(createInstallGuidePanel(project, this), BorderLayout.NORTH);
        panel.add(splitter, BorderLayout.CENTER);
        setContent(panel);

        this.appMapTreePanelPath = List.of(panel, splitter, appMapPanel, tree);
    }

    @Override
    public void dispose() {
        LOG.debug("disposing AppMap tool window");
    }

    /**
     * Creates a panel with the search text field and the start and stop actions.
     */
    private @NotNull JComponent createToolBar(@NotNull AppMapModel appMapModel) {
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

    @Override
    public @Nullable Object getData(@NotNull @NonNls String dataId) {
        // don't provide PsiFile on the EDT, but delegate to the slow providers
        if (CommonDataKeys.PSI_FILE.is(dataId) || CommonDataKeys.NAVIGATABLE.is(dataId)) {
            return null;
        }

        if (PlatformCoreDataKeys.BGT_DATA_PROVIDER.is(dataId)) {
            return (DataProvider) this::getDataImpl;
        }
        return getDataImpl(dataId);
    }

    @Override
    public void onToolWindowShown() {
        LOG.debug("onToolWindowShown");
        this.isToolWindowVisible = true;

        if (hasPendingTreeRefresh) {
            LOG.debug("Triggering pending refresh of AppMap tool window");
            rebuild(true);
        }
    }

    @Override
    public void onToolWindowHidden() {
        LOG.debug("onToolWindowHidden");
        this.isToolWindowVisible = false;
    }

    /**
     * Make the tree of AppMaps visible and make it the focused component.
     */
    public void showAppMapTreePanel() {
        var componentPath = appMapTreePanelPath;
        if (componentPath.isEmpty()) {
            return;
        }

        // collapse all collapsible panels to reset the view
        collapseChildren(getContent());

        // make the hierarchy visible
        Component previous = null;
        for (var c : componentPath) {
            c.setVisible(true);

            if (c instanceof CollapsiblePanel) {
                ((CollapsiblePanel) c).setCollapsed(false);
            } else if (c instanceof Splitter && previous != null) {
                var splitter = (Splitter) c;
                // 1.0 to display the first component, 0.0 to display the other component
                splitter.setProportion(previous.equals(splitter.getFirstComponent()) ? 1.0f : 0.0f);
            }

            previous = c;
        }

        // focus on the last component in the path, i.e. the tree of AppMaps
        appMapTreePanelPath.get(appMapTreePanelPath.size() - 1).requestFocusInWindow();
    }

    private @Nullable Object getDataImpl(@NotNull @NonNls String dataId) {
        if (PlatformDataKeys.DELETE_ELEMENT_PROVIDER.is(dataId)) {
            return deleteDataProvider;
        }

        if (KEY_ALL_APPMAPS.is(dataId)) {
            var root = tree.getModel().getRoot();
            if (root != null) {
                assert root instanceof Node;
                return ((Node) root).getFiles().toArray(VirtualFile.EMPTY_ARRAY);
            }
        }

        if (PlatformCoreDataKeys.PROJECT.is(dataId)) {
            return project;
        }

        if (CommonDataKeys.VIRTUAL_FILE.is(dataId)) {
            return getSelectedFile();
        }

        if (CommonDataKeys.VIRTUAL_FILE_ARRAY.is(dataId)) {
            return getSelectedFiles();
        }

        if (CommonDataKeys.NAVIGATABLE.is(dataId) || CommonDataKeys.PSI_FILE.is(dataId)) {
            var file = getSelectedFile();
            return file != null ? PsiUtilCore.getPsiFile(project, file) : null;
        }

        return super.getData(dataId);
    }

    private @Nullable VirtualFile getSelectedFile() {
        var path = tree.getSelectionPath();
        var file = path != null && path.getLastPathComponent() instanceof Node
                ? ((Node) path.getLastPathComponent()).getFile()
                : null;
        return file != null && file.isValid() ? file : null;
    }

    /**
     * @return Files of the selected items. If a selected item is a group of AppMaps,
     * then all child AppMaps are added to the result.
     */
    private @Nullable VirtualFile[] getSelectedFiles() {
        var paths = tree.getSelectionPaths();
        if (paths == null || paths.length == 0) {
            return null;
        }

        var files = new HashSet<VirtualFile>();
        for (var path : paths) {
            var pathItem = path.getLastPathComponent();
            if (pathItem instanceof Node) {
                files.addAll(((Node) pathItem).getFiles());
            }
        }
        return files.stream().filter(VirtualFile::isValid).toArray(VirtualFile[]::new);
    }

    private @NotNull SearchTextField createNameFilter(@NotNull AppMapModel appMapModel) {
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

    private void rebuild(boolean force) {
        if (force || isToolWindowVisible) {
            hasPendingTreeRefresh = false;
            treeRefreshAlarm.cancelAndRequest();
        } else {
            LOG.debug("rebuild with hidden AppMap tool window");
            hasPendingTreeRefresh = true;
        }
    }

    private void refreshAndExpand(@NotNull AppMapModel appMapModel) {
        appMapModel.refresh();
        appMapModel.getInvoker().invokeLater(() -> TreeUtil.expand(this.tree, 3));
    }

    private @NotNull JComponent createContentPanel(@NotNull Project project,
                                                   @NotNull JComponent viewport,
                                                   @NotNull Disposable parent,
                                                   @NotNull AppMapModel appMapModel) {
        var appMapPanel = createAppMapPanel(project, viewport, appMapModel);
        var splitter = createSplitter();
        splitter.setFirstComponent(appMapPanel);
        splitter.setSecondComponent(createSouthPanel(project, parent));

        var panel = new JPanel(new BorderLayout());
        panel.add(createInstallGuidePanel(project, parent), BorderLayout.NORTH);
        panel.add(splitter, BorderLayout.CENTER);

        this.appMapTreePanelPath = List.of(panel, splitter, appMapPanel, viewport);

        return panel;
    }

    /**
     * @return Splitter, which distributes the remaining space to the other component if a component has its maximum size set.
     */
    @NotNull
    private static OnePixelSplitter createSplitter() {
        return new OnePixelSplitter(true, "appmap.toolWindow.state", 0.75f) {
            @Override
            public void doLayout() {
                super.doLayout();

                var first = getFirstComponent();
                var second = getSecondComponent();
                if (first != null && first.isVisible() && second != null && second.isVisible()) {
                    assert isVertical();

                    int height = getHeight();
                    var d = getDividerWidth();
                    if (height > d) {
                        var maxSize1 = first.getMaximumSize();
                        var maxHeight1 = maxSize1 != null && maxSize1.getHeight() < Integer.MAX_VALUE
                                ? maxSize1.getHeight()
                                : null;
                        var maxSize2 = second.getMaximumSize();
                        var maxHeight2 = maxSize2 != null && maxSize2.getHeight() < Integer.MAX_VALUE
                                ? maxSize2.getHeight()
                                : null;
                        var height1Invalid = maxHeight1 != null && first.getHeight() > maxHeight1;
                        var height2Invalid = maxHeight2 != null && second.getHeight() > maxHeight2;

                        int height1;
                        int height2;
                        if (maxHeight1 != null && maxHeight2 != null || height1Invalid) {
                            // distribute available space to 2nd component,
                            // because we don't want to show a large "AppMaps" panel without content
                            height1 = (int) Math.round(maxHeight1);
                            height2 = height - height1 - d;
                        } else if (height2Invalid) {
                            // limit height of 2nd component to its max height
                            height2 = (int) Math.round(maxHeight2);
                            height1 = height - height2 - d;
                        } else {
                            // no update needed
                            return;
                        }

                        int width = getWidth();
                        var firstRect = new Rectangle(0, 0, width, height1);
                        var dividerRect = new Rectangle(0, height1, width, d);
                        var secondRect = new Rectangle(0, height1 + d, width, height2);

                        myDivider.setVisible(true);

                        first.setBounds(firstRect);
                        myDivider.setBounds(dividerRect);
                        second.setBounds(secondRect);
                    }
                }
            }
        };
    }

    private static @NotNull SimpleTree createTree(@NotNull Project project,
                                                  @NotNull Disposable disposable,
                                                  @NotNull TreeModel treeModel) {
        var tree = new SimpleTree(new AsyncTreeModel(treeModel, disposable));
        tree.getEmptyText().appendLine(AppMapBundle.get("toolwindow.appmap.emptyText.line1"));
        tree.getEmptyText().appendLine(AppMapBundle.get("toolwindow.appmap.emptyText.line2"));
        tree.getEmptyText().appendLine(
                AppMapBundle.get("toolwindow.appmap.installAgentEmptyText"),
                SimpleTextAttributes.LINK_ATTRIBUTES,
                e -> InstallGuideEditorProvider.open(project, InstallGuideViewPage.InstallAgent));
        tree.setRootVisible(false);
        tree.setShowsRootHandles(false);

        TreeUtil.installActions(tree);
        EditSourceOnDoubleClickHandler.install(tree);
        EditSourceOnEnterKeyHandler.install(tree);

        PopupHandler.installPopupMenu(tree, createPopupGroup(tree), ActionPlaces.TOOLWINDOW_POPUP);

        return tree;
    }

    private static @NotNull ActionGroup createPopupGroup(@NotNull JTree tree) {
        var actions = new DefaultActionGroup();

        // our own actions
        actions.add(createDeleteAppMapsAction());
        actions.add(new DeleteAllMapsAction());

        // default actions to work with trees
        var expander = new DefaultTreeExpander(tree);
        var commonActionsManager = CommonActionsManager.getInstance();

        actions.add(Separator.getInstance());
        actions.add(Objects.requireNonNull(ActionManager.getInstance().getActionOrStub(IdeActions.ACTION_EDIT_SOURCE)));
        actions.addAction(commonActionsManager.createExpandAllAction(expander, tree));
        actions.addAction(commonActionsManager.createCollapseAllAction(expander, tree));

        return actions;
    }

    private static @NotNull DeleteAction createDeleteAppMapsAction() {
        var action = new DeleteAction() {
            @Override
            public void update(@NotNull AnActionEvent e) {
                super.update(e);
                e.getPresentation().setIcon(Icons.APPMAP_FILE);
            }
        };
        ActionUtil.copyFrom(action, IdeActions.ACTION_DELETE);
        return action;
    }

    private JPanel createAppMapPanel(@NotNull Project project,
                                     @NotNull JComponent viewport,
                                     @NotNull AppMapModel appMapModel) {
        var appMapListPanel = ScrollPaneFactory.createScrollPane(viewport, true);
        appMapListPanel.setMinimumSize(new JBDimension(0, 200));

        var panelWithFilter = new JPanel(new BorderLayout());
        panelWithFilter.add(createToolBar(appMapModel), BorderLayout.NORTH);
        panelWithFilter.add(appMapListPanel, BorderLayout.CENTER);

        return new appland.toolwindow.CollapsiblePanel(project,
                AppMapBundle.get("toolwindow.appmap.appMaps"),
                "appmap.toolWindow.appMaps.collapsed",
                true,
                panelWithFilter);
    }

    private void collapseChildren(@Nullable Container container) {
        if (container == null) {
            return;
        }

        for (var childComponent : container.getComponents()) {
            if (childComponent instanceof CollapsiblePanel) {
                ((CollapsiblePanel) childComponent).setCollapsed(true);
            } else if (childComponent instanceof Container) {
                collapseChildren((Container) childComponent);
            }
        }
    }

    private static @NotNull JComponent createSouthPanel(@NotNull Project project, @NotNull Disposable parent) {
        var contentPanel = new VerticalBox();
        contentPanel.add(createRuntimeAnalysisPanel(project, parent));
        contentPanel.add(createCodeObjectsPanel(project, parent));
        contentPanel.add(createDocumentationLinksPanel(project));
        return contentPanel;
    }

    @NotNull
    private static JPanel createInstallGuidePanel(@NotNull Project project, @NotNull Disposable parent) {
        return new appland.toolwindow.CollapsiblePanel(project,
                AppMapBundle.get("toolwindow.appmap.instructions"),
                "appmap.toolWindow.installGuide.collapsed",
                false,
                new InstallGuidePanel(project, parent));
    }

    @NotNull
    private static JPanel createRuntimeAnalysisPanel(@NotNull Project project, @NotNull Disposable parent) {
        var runtimeAnalysisPanel = new RuntimeAnalysisPanel(project, parent);
        runtimeAnalysisPanel.setMinimumSize(new JBDimension(0, 100));
        return new appland.toolwindow.CollapsiblePanel(project,
                AppMapBundle.get("toolwindow.appmap.runtimeAnalysis"),
                "appmap.toolWindow.runtimeAnalysis.collapsed",
                true,
                runtimeAnalysisPanel);
    }

    @NotNull
    private static JPanel createCodeObjectsPanel(@NotNull Project project, @NotNull Disposable parentDisposable) {
        var codeObjectsPanel = new CodeObjectsPanel(project, parentDisposable);
        codeObjectsPanel.setMinimumSize(new JBDimension(0, 100));
        return new appland.toolwindow.CollapsiblePanel(project,
                AppMapBundle.get("toolwindow.appmap.codeObjects"),
                "appmap.toolWindow.codeObjects.collapsed",
                true,
                codeObjectsPanel);
    }

    @NotNull
    private static JPanel createDocumentationLinksPanel(@NotNull Project project) {
        return new CollapsiblePanel(project,
                AppMapBundle.get("toolwindow.appmap.documentation"),
                "appmap.toolWindow.documentation.collapsed",
                true,
                new AppMapContentPanel(true) {
                    @Override
                    protected void setupPanel() {
                        /* can be generated from vscode links with:
                         * $ curl -L https://github.com/getappmap/vscode-appland/raw/master/src/tree/links.ts |
                         *   sed -n '/label:/ {s/.*: /add(new UrlLabel(/; s/,\n/, /; N; s/\n *link: / /; s/,$/\)\);/; y/'"'"'/"/; p }'
                         */
                        add(new UrlLabel("Quickstart", "https://appmap.io/docs/quickstart"));
                        add(new UrlLabel("AppMap overview", "https://appmap.io/docs/appmap-overview"));
                        add(new UrlLabel("How to use AppMap diagrams", "https://appmap.io/docs/how-to-use-appmap-diagrams"));
                        add(new UrlLabel("Sequence diagrams", "https://appmap.io/docs/diagrams/sequence-diagrams.html"));
                        add(new UrlLabel("Reference", "https://appmap.io/docs/reference"));
                        add(new UrlLabel("Recording methods", "https://appmap.io/docs/recording-methods"));
                        add(new UrlLabel("Community", "https://appmap.io/docs/community"));
                    }
                });
    }

    /**
     * Delete handler to override the Action title shown in the popup of the AppMap tree and in the edit menu of the IDE.
     */
    private static final class AppMapDeleteProvider implements DeleteProvider, TitledHandler {
        private final DeleteProvider delegate = new VirtualFileDeleteProvider();

        @Override
        public @NlsActions.ActionText String getActionTitle() {
            return AppMapBundle.get("toolwindow.appmap.actions.deleteAppMap.title");
        }

        @Override
        public boolean canDeleteElement(@NotNull DataContext dataContext) {
            return delegate.canDeleteElement(dataContext);
        }

        @Override
        public void deleteElement(@NotNull DataContext dataContext) {
            delegate.deleteElement(dataContext);
        }
    }
}
