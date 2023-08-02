package appland.toolwindow;

import appland.AppMapBundle;
import appland.actions.StartAppMapRecordingAction;
import appland.actions.StopAppMapRecordingAction;
import appland.index.AppMapMetadata;
import appland.index.IndexedFileListenerUtil;
import appland.installGuide.InstallGuideEditorProvider;
import appland.installGuide.InstallGuideViewPage;
import appland.toolwindow.codeObjects.CodeObjectsPanel;
import appland.toolwindow.installGuide.InstallGuidePanel;
import appland.toolwindow.installGuide.UrlLabel;
import appland.toolwindow.runtimeAnalysis.RuntimeAnalysisPanel;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiManager;
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
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.List;

public class AppMapWindowPanel extends SimpleToolWindowPanel implements DataProvider, Disposable, AppMapToolWindowContent {
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

    public AppMapWindowPanel(@NotNull Project project, @NotNull Disposable parent) {
        super(true);
        Disposer.register(parent, this);

        var appMapModel = new AppMapModel(project);
        this.project = project;
        this.tree = createTree(project, this, appMapModel);
        this.treeRefreshAlarm = new SingleAlarm(appMapModel::refresh, TREE_REFRESH_DELAY_MILLIS, this, Alarm.ThreadToUse.POOLED_THREAD);

        setToolbar(createToolBar(appMapModel));
        setContent(createContentPanel(project, tree, this));

        IndexedFileListenerUtil.registerListeners(project, this, true, false, () -> rebuild(false));
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
        var selectedFile = getSelectedFile();

        if (PlatformCoreDataKeys.SLOW_DATA_PROVIDERS.is(dataId)) {
            return List.of((DataProvider) id -> {
                if (CommonDataKeys.NAVIGATABLE.is(id) && selectedFile != null) {
                    return PsiManager.getInstance(project).findFile(selectedFile);
                }
                return null;
            });
        }

        if (CommonDataKeys.VIRTUAL_FILE.is(dataId)) {
            return selectedFile;
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

    private @NotNull SearchTextField createNameFilter(@NotNull AppMapModel appMapModel) {
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

    private void rebuild(boolean force) {
        if (force || isToolWindowVisible) {
            hasPendingTreeRefresh = false;
            treeRefreshAlarm.cancelAndRequest();
        } else {
            LOG.debug("rebuild with hidden AppMap tool window");
            hasPendingTreeRefresh = true;
        }
    }

    private static @NotNull JComponent createContentPanel(@NotNull Project project,
                                                          @NotNull JComponent viewport,
                                                          @NotNull Disposable parent) {
        var splitter = createSplitter();
        splitter.setFirstComponent(createAppMapPanel(project, viewport));
        splitter.setSecondComponent(createSouthPanel(project, parent));

        var panel = new JPanel(new BorderLayout());
        panel.add(createInstallGuidePanel(project, parent), BorderLayout.NORTH);
        panel.add(splitter, BorderLayout.CENTER);
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
                        var maxSize2 = second.getMaximumSize();

                        int iSize1;
                        int iSize2;
                        if (maxSize1 != null && first.getHeight() > maxSize1.height) {
                            // limit height of 1st component to its max height
                            iSize1 = (int) Math.round(maxSize1.getHeight());
                            iSize2 = height - iSize1 - d;
                        } else if (maxSize2 != null && second.getHeight() > maxSize2.height) {
                            // limit height of 2nd component to its max height
                            iSize2 = (int) Math.round(maxSize2.getHeight());
                            iSize1 = height - iSize2 - d;
                        } else {
                            // no update needed
                            return;
                        }

                        int width = getWidth();
                        var firstRect = new Rectangle(0, 0, width, iSize1);
                        var dividerRect = new Rectangle(0, iSize1, width, d);
                        var secondRect = new Rectangle(0, iSize1 + d, width, iSize2);

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
        tree.setCellRenderer(new AppMapModel.TreeCellRenderer());
        tree.getEmptyText().appendLine(AppMapBundle.get("toolwindow.appmap.emptyText.line1"));
        tree.getEmptyText().appendLine(AppMapBundle.get("toolwindow.appmap.emptyText.line2"));
        tree.getEmptyText().appendLine(
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

    private static JPanel createAppMapPanel(@NotNull Project project, @NotNull JComponent viewport) {
        var appMapListPanel = ScrollPaneFactory.createScrollPane(viewport, true);
        appMapListPanel.setMinimumSize(new JBDimension(0, 200));
        return new CollapsiblePanel(project,
                AppMapBundle.get("toolwindow.appmap.appMaps"),
                "appmap.toolWindow.appMaps.collapsed",
                true,
                appMapListPanel);
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
        return new CollapsiblePanel(project,
                AppMapBundle.get("toolwindow.appmap.instructions"),
                "appmap.toolWindow.installGuide.collapsed",
                false,
                new InstallGuidePanel(project, parent));
    }

    @NotNull
    private static JPanel createRuntimeAnalysisPanel(@NotNull Project project, @NotNull Disposable parent) {
        var runtimeAnalysisPanel = new RuntimeAnalysisPanel(project, parent);
        runtimeAnalysisPanel.setMinimumSize(new JBDimension(0, 100));
        return new CollapsiblePanel(project,
                AppMapBundle.get("toolwindow.appmap.runtimeAnalysis"),
                "appmap.toolWindow.runtimeAnalysis.collapsed",
                true,
                runtimeAnalysisPanel);
    }

    @NotNull
    private static JPanel createCodeObjectsPanel(@NotNull Project project, @NotNull Disposable parentDisposable) {
        var codeObjectsPanel = new CodeObjectsPanel(project, parentDisposable);
        codeObjectsPanel.setMinimumSize(new JBDimension(0, 100));
        return new CollapsiblePanel(project,
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
}
