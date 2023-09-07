package appland.toolwindow.installGuide;

import appland.config.AppMapConfigFileListener;
import appland.index.AppMapNameIndex;
import appland.index.AppMapSearchScopes;
import appland.installGuide.InstallGuideViewPage;
import appland.installGuide.projectData.ProjectDataService;
import appland.installGuide.projectData.ProjectMetadata;
import appland.problemsView.listener.ScannerFindingsListener;
import appland.settings.AppMapProjectSettingsService;
import appland.settings.AppMapSettingsListener;
import appland.toolwindow.AppMapContentPanel;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.util.Alarm;
import com.intellij.util.SingleAlarm;
import com.intellij.util.concurrency.AppExecutorUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static appland.installGuide.InstallGuideEditorProvider.open;

/**
 * Manages a list of collapsible user milestone panels.
 */
public class InstallGuidePanel extends AppMapContentPanel implements Disposable {
    private final Project project;
    private final List<StatusLabel> statusLabels;
    // debounce label updates by 500ms
    private final SingleAlarm labelRefreshAlarm = new SingleAlarm(this::refreshInitialStatus, 500, this, Alarm.ThreadToUse.SWING_THREAD);

    public InstallGuidePanel(@NotNull Project project, @NotNull Disposable parent) {
        super(false);
        Disposer.register(parent, this);

        this.project = project;
        this.statusLabels = Arrays.stream(InstallGuideViewPage.values())
                .map(page -> new StatusLabel(page, () -> open(project, page)))
                .collect(Collectors.toList());
        setupPanel();
    }

    @Override
    protected void setupPanel() {
        statusLabels.forEach(this::add);
        refreshInitialStatus();
        registerStatusUpdateListeners(project, this, statusLabels);
    }

    private void triggerLabelStatusUpdate() {
        labelRefreshAlarm.cancelAndRequest();
    }

    private void refreshInitialStatus() {
        for (var label : statusLabels) {
            updateLabelStatus(project, label);
        }
    }

    /**
     * if findings are enabled, completed if at least one appmap-findings.json file was found
     */
    private static void updateRuntimeAnalysisLabel(@NotNull Project project, @NotNull StatusLabel label) {
        var hasFindings = AppMapProjectSettingsService.getState(project).isInvestigatedFindings();
        label.setStatus(hasFindings ? InstallGuideStatus.Completed : InstallGuideStatus.Incomplete);
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
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            // Always mark supported Java projects as supported, but only if all modules are supported.
            // isAgentInstalled is already checking for supported Java projects and the presence of appmap.yml.
            var anySupported = ProjectDataService.getInstance(project).getAppMapProjects()
                    .stream()
                    .anyMatch(ProjectMetadata::isAgentInstalled);

            ApplicationManager.getApplication().invokeLater(() -> {
                label.setStatus(anySupported ? InstallGuideStatus.Completed : InstallGuideStatus.Incomplete);
            });
        });
    }

    /**
     * presence of at least one .appmap.json file
     */
    private static void updateRecordAppMapsLabel(@NotNull Project project, @NotNull StatusLabel label) {
        findIndexedStatus(project, label, () -> !AppMapNameIndex.isEmpty(project, AppMapSearchScopes.appMapsWithExcluded(project)));
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

    private void registerStatusUpdateListeners(@NotNull Project project,
                                               @NotNull Disposable parent,
                                               @NotNull List<StatusLabel> labels) {
        var connection = project.getMessageBus().connect(parent);

        connection.subscribe(AppMapSettingsListener.TOPIC, new AppMapSettingsListener() {
            @Override
            public void apiKeyChanged() {
                triggerLabelStatusUpdate();
            }
        });

        connection.subscribe(ScannerFindingsListener.TOPIC, new ScannerFindingsListener() {
            @Override
            public void afterFindingsReloaded() {
                triggerLabelStatusUpdate();
            }

            @Override
            public void afterFindingsChanged() {
                triggerLabelStatusUpdate();
            }
        });

        connection.subscribe(AppMapSettingsListener.TOPIC, new AppMapSettingsListener() {
            @Override
            public void createOpenApiChanged() {
                triggerLabelStatusUpdate();
            }

            @Override
            public void openedAppMapChanged() {
                triggerLabelStatusUpdate();
            }

            @Override
            public void investigatedFindingsChanged() {
                triggerLabelStatusUpdate();
            }
        });

        connection.subscribe(AppMapConfigFileListener.TOPIC, this::triggerLabelStatusUpdate);
    }

    /**
     * Executes the supplied under a non-blocking read action and updates the label on the EDT.
     */
    private static void findIndexedStatus(@NotNull Project project,
                                          @NotNull StatusLabel label,
                                          @NotNull Supplier<Boolean> statusSupplier) {
        ReadAction.nonBlocking(statusSupplier::get)
                .inSmartMode(project)
                .finishOnUiThread(ModalityState.defaultModalityState(), found -> {
                    label.setStatus(found ? InstallGuideStatus.Completed : InstallGuideStatus.Incomplete);
                })
                .submit(AppExecutorUtil.getAppExecutorService());
    }
}
