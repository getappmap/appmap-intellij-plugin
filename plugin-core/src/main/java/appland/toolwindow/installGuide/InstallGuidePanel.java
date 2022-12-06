package appland.toolwindow.installGuide;

import appland.config.AppMapConfigFileListener;
import appland.files.AppMapFileChangeListener;
import appland.files.AppMapFiles;
import appland.index.AppMapMetadataIndex;
import appland.installGuide.InstallGuideViewPage;
import appland.problemsView.FindingsManager;
import appland.problemsView.listener.ScannerFindingsListener;
import appland.settings.AppMapApplicationSettingsService;
import appland.settings.AppMapProjectSettingsService;
import appland.settings.AppMapSettingsListener;
import appland.toolwindow.AppMapContentPanel;
import appland.toolwindow.CollapsibleAppMapPanel;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ui.components.panels.VerticalLayout;
import com.intellij.util.concurrency.AppExecutorUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static appland.installGuide.InstallGuideEditorProvider.open;

/**
 * Manages a list of collapsible user milestone panels.
 */
public class InstallGuidePanel extends JPanel implements Disposable {
    public InstallGuidePanel(@NotNull Project project, @NotNull Disposable parent) {
        super(new VerticalLayout(5));
        Disposer.register(parent, this);

        var statusLabels = Arrays.stream(InstallGuideViewPage.values())
                .map(page -> new StatusLabel(page, () -> open(project, page)))
                .collect(Collectors.toList());

        add(new CollapsibleAppMapPanel("Quickstart", false, new AppMapContentPanel() {
            @Override
            protected void setupPanel() {
                statusLabels.forEach(this::add);
            }
        }));

        refreshInitialStatus(project, statusLabels);

        registerStatusUpdateListeners(project, this, statusLabels);
    }

    private void refreshInitialStatus(@NotNull Project project, @NotNull List<StatusLabel> labels) {
        for (var label : labels) {
            updateLabelStatus(project, label);
        }
    }

    private void registerStatusUpdateListeners(@NotNull Project project,
                                               @NotNull Disposable parent,
                                               @NotNull List<StatusLabel> labels) {
        var connection = project.getMessageBus().connect(parent);

        // general settings change handling
        connection.subscribe(AppMapSettingsListener.TOPIC, new AppMapSettingsListener() {
            @Override
            public void apiKeyChanged() {
                refreshItems();
            }

            @Override
            public void enableFindingsChanged() {
                refreshItems();
            }

            private void refreshItems() {
                refreshInitialStatus(project, labels);
            }
        });

        // per-page handling
        for (var label : labels) {
            switch (label.getPage()) {
                case InstallAgent:
                    connection.subscribe(AppMapConfigFileListener.TOPIC, () -> updateInstallAgentLabel(project, label));
                    break;

                case RecordAppMaps:
                    connection.subscribe(AppMapFileChangeListener.TOPIC, changes -> updateRecordAppMapsLabel(project, label));
                    break;

                case OpenAppMaps:
                    connection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {
                        @Override
                        public void selectionChanged(@NotNull FileEditorManagerEvent event) {
                            if (event.getNewFile() != null && AppMapFiles.isAppMap(event.getNewFile())) {
                                updateOpenAppMapsLabel(project, label);
                            }
                        }
                    });
                    break;

                case GenerateOpenAPI:
                    connection.subscribe(AppMapSettingsListener.TOPIC, new AppMapSettingsListener() {
                        @Override
                        public void createOpenApiChanged() {
                            updateGenerateOpenApiLabel(project, label);
                        }
                    });
                    break;

                case RuntimeAnalysis:
                    project.getMessageBus().connect(parent).subscribe(ScannerFindingsListener.TOPIC, new ScannerFindingsListener() {
                        @Override
                        public void afterFindingsReloaded() {
                            updateRuntimeAnalysisLabel(project, label);
                        }

                        @Override
                        public void afterFindingsChanged() {
                            updateRuntimeAnalysisLabel(project, label);
                        }
                    });
                    break;
            }
        }
    }

    private void updateLabelStatus(@NotNull Project project, @NotNull StatusLabel label) {
        switch (label.getPage()) {
            case InstallAgent:
                updateInstallAgentLabel(project, label);
                break;

            case RecordAppMaps:
                updateRecordAppMapsLabel(project, label);
                break;

            case OpenAppMaps:
                updateOpenAppMapsLabel(project, label);
                break;

            case GenerateOpenAPI:
                updateGenerateOpenApiLabel(project, label);
                break;

            case RuntimeAnalysis:
                updateRuntimeAnalysisLabel(project, label);
                break;
        }
    }

    @Override
    public void dispose() {
    }

    /**
     * Presence of at least one appmap.yml file.
     */
    private static void updateInstallAgentLabel(@NotNull Project project, @NotNull StatusLabel label) {
        findIndexedStatus(project, label, () -> {
            return FilenameIndex.processFilesByName(AppMapFiles.APPMAP_YML, false, file -> {
                return false;
            }, GlobalSearchScope.everythingScope(project), project);
        });
    }

    /**
     * presence of at least one .appmap.json file
     */
    private static void updateRecordAppMapsLabel(@NotNull Project project, @NotNull StatusLabel label) {
        findIndexedStatus(project, label, () -> {
            var found = new AtomicBoolean(false);
            AppMapMetadataIndex.processAppMaps(project, GlobalSearchScope.everythingScope(project), (file, appmap) -> {
                found.set(true);
                return false;
            });
            return found.get();
        });
    }

    /**
     * if the AppMap webview was at least shown once
     */
    private static void updateOpenAppMapsLabel(@NotNull Project project, @NotNull StatusLabel label) {
        label.setStatus(AppMapProjectSettingsService.getState(project).isOpenedAppMapEditor()
                ? InstallGuideStatus.Completed
                : InstallGuideStatus.Incomplete);
    }

    private static void updateGenerateOpenApiLabel(@NotNull Project project, @NotNull StatusLabel label) {
        label.setStatus(AppMapProjectSettingsService.getState(project).isCreatedOpenAPI()
                ? InstallGuideStatus.Completed
                : InstallGuideStatus.Incomplete);
    }

    /**
     * if findings are enabled, completed if at least one appmap-findings.json file was found
     */
    private static void updateRuntimeAnalysisLabel(@NotNull Project project, @NotNull StatusLabel label) {
        if (!AppMapApplicationSettingsService.getInstance().isAnalysisEnabled()) {
            label.setStatus(InstallGuideStatus.Unavailable);
            return;
        }

        DumbService.getInstance(project).runWhenSmart(() -> {
            var manager = FindingsManager.getInstance(project);
            var hasFindings = manager.getProblemFileCount() > 0 || manager.getOtherProblemCount() > 0;
            label.setStatus(hasFindings ? InstallGuideStatus.Completed : InstallGuideStatus.Incomplete);
        });
    }

    /**
     * Executes the supplied under a non-blocking read action and updates the label on the EDT.
     */
    private static void findIndexedStatus(@NotNull Project project,
                                          @NotNull StatusLabel label,
                                          @NotNull Supplier<Boolean> statusSupplier) {
        ReadAction.nonBlocking(statusSupplier::get)
                .inSmartMode(project)
                .finishOnUiThread(ModalityState.current(), found -> {
                    label.setStatus(found ? InstallGuideStatus.Completed : InstallGuideStatus.Incomplete);
                })
                .submit(AppExecutorUtil.getAppExecutorService());
    }
}
