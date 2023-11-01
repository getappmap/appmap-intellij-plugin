package appland.toolwindow.installGuide;

import appland.config.AppMapConfigFileListener;
import appland.index.AppMapFindingsUtil;
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
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.util.Alarm;
import com.intellij.util.SingleAlarm;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
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
    private final SingleAlarm labelRefreshAlarm = new SingleAlarm(this::refreshInitialStatus, 500, this, Alarm.ThreadToUse.POOLED_THREAD);

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
    public void dispose() {
    }

    @Override
    protected void setupPanel() {
        statusLabels.forEach(this::add);
        registerStatusUpdateListeners(project, this, statusLabels);
        labelRefreshAlarm.request(true);
    }

    private void triggerLabelStatusUpdate() {
        labelRefreshAlarm.cancelAndRequest();
    }

    @RequiresBackgroundThread
    private void refreshInitialStatus() {
        // Force a refresh of the metadata outside the non-blocking ReadAction below
        // The refresh must not be executed within a ReadAction.
        ProjectDataService.getInstance(project).getAppMapProjects(true);

        ReadAction.nonBlocking(() -> {
                    return statusLabels.stream().collect(Collectors.toMap(Function.identity(), label -> {
                        return updateLabelStatus(project, label);
                    }));
                })
                .coalesceBy(this)
                .expireWith(this)
                .inSmartMode(project)
                .finishOnUiThread(ModalityState.any(), statusMapping -> {
                            for (var entry : statusMapping.entrySet()) {
                                entry.getKey().setStatus(entry.getValue());
                            }
                        }
                )
                .submit(AppExecutorUtil.getAppExecutorService());
    }

    /**
     * @param project Current project
     * @param label   Label to check
     * @return {@code true} if the label should be enabled, {@code false} if it should be disabled
     */
    private @NotNull InstallGuideStatus updateLabelStatus(@NotNull Project project, @NotNull StatusLabel label) {
        switch (label.getPage()) {
            case InstallAgent:
                return updateInstallAgentLabel(project);
            case RecordAppMaps:
                return updateRecordAppMapsLabel(project);
            case OpenAppMaps:
                return updateOpenAppMapsLabel(project);
            case RuntimeAnalysis:
                return updateRuntimeAnalysisLabel(project);
            default:
                throw new IllegalStateException("unexpected label: " + label);
        }
    }

    /**
     * Presence of at least one appmap.yml file.
     */
    private static @NotNull InstallGuideStatus updateInstallAgentLabel(@NotNull Project project) {
        // Always mark supported Java projects as supported, but only if all modules are supported.
        // isAgentInstalled is already checking for supported Java projects and the presence of appmap.yml.
        var anySupported = ProjectDataService.getInstance(project).getAppMapProjects(false)
                .stream()
                .anyMatch(ProjectMetadata::isAgentInstalled);
        return anySupported ? InstallGuideStatus.Completed : InstallGuideStatus.Incomplete;
    }

    /**
     * Presence of at least one .appmap.json file.
     */
    private static @NotNull InstallGuideStatus updateRecordAppMapsLabel(@NotNull Project project) {
        return !AppMapNameIndex.isEmpty(project, AppMapSearchScopes.appMapsWithExcluded(project))
                ? InstallGuideStatus.Completed
                : InstallGuideStatus.Incomplete;
    }

    /**
     * If the AppMap webview was at least shown once
     */
    private static @NotNull InstallGuideStatus updateOpenAppMapsLabel(@NotNull Project project) {
        return AppMapProjectSettingsService.getState(project).isOpenedAppMapEditor()
                ? InstallGuideStatus.Completed
                : InstallGuideStatus.Incomplete;
    }

    /**
     * The label is updated under the following conditions:
     * - The user navigated to the "Runtime Analysis" step in the installation guide webview
     * - AppMaps have been opened
     * - There are AppMaps in the project, and they have been scanned, regardless if findings were detected
     */
    private static @NotNull InstallGuideStatus updateRuntimeAnalysisLabel(@NotNull Project project) {
        var navigated = AppMapProjectSettingsService.getState(project).isInvestigatedFindings();
        var openedAppMaps = AppMapProjectSettingsService.getState(project).isOpenedAppMapEditor();
        var scannedAppMaps = AppMapFindingsUtil.isAnalysisPerformed(AppMapSearchScopes.projectFilesWithExcluded(project));
        return navigated && openedAppMaps && scannedAppMaps
                ? InstallGuideStatus.Completed
                : InstallGuideStatus.Incomplete;
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
