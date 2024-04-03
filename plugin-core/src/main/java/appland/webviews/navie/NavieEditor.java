package appland.webviews.navie;

import appland.AppMapBundle;
import appland.files.AppMapFileChangeListener;
import appland.files.OpenAppMapFileNavigatable;
import appland.index.AppMapMetadata;
import appland.index.AppMapMetadataService;
import appland.installGuide.InstallGuideEditorProvider;
import appland.installGuide.InstallGuideViewPage;
import appland.settings.AppMapApplicationSettingsService;
import appland.settings.AppMapProjectSettingsService;
import appland.settings.AppMapSettingsListener;
import appland.toolwindow.AppMapToolWindowFactory;
import appland.utils.GsonUtils;
import appland.webviews.SharedAppMapWebViewMessages;
import appland.webviews.WebviewEditor;
import appland.webviews.webserver.AppMapWebview;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Alarm;
import com.intellij.util.SingleAlarm;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.concurrency.annotations.RequiresReadLock;
import lombok.Value;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

public class NavieEditor extends WebviewEditor<Void> {
    // debounce requests to update the most recent updates by 1s
    private final SingleAlarm refreshMostRecentAppMapsAlarm = new SingleAlarm(
            this::updateMostRecentAppMaps,
            1_000,
            this,
            Alarm.ThreadToUse.POOLED_THREAD);
    // limit updates of most recent AppMaps to a single thread for predictable ordering of the updates
    private final Executor updateAppMapsThread = AppExecutorUtil.createBoundedApplicationPoolExecutor("Navie AppMap updater", 1);

    public NavieEditor(@NotNull Project project, @NotNull VirtualFile file) {
        super(project, AppMapWebview.Navie, file, SharedAppMapWebViewMessages.withBaseMessages(
                "open-appmap",
                "open-install-instructions",
                "open-record-instructions",
                "show-appmap-tree"));
    }

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) @NotNull String getName() {
        return AppMapBundle.get("webview.navie.title");
    }

    @Override
    protected void setupInitMessage(@Nullable Void initData, @NotNull JsonObject payload) {
        var apiKey = AppMapApplicationSettingsService.getInstance().getApiKey();
        var filters = AppMapProjectSettingsService.getState(project).getAppMapFilters().values();
        var codeSelection = NavieEditorProvider.KEY_CODE_SELECTION.get(file);

        var port = NavieEditorProvider.KEY_INDEXER_RPC_PORT.get(file);
        assert port != null;

        var mostRecentAppMaps = ReadAction.compute(() -> findMostRecentAppMaps(project));

        payload.addProperty("appmapRpcPort", port);
        payload.addProperty("apiKey", StringUtil.defaultIfEmpty(apiKey, ""));

        // At this time, the Navie editor can only be opened after selecting an available AppMap directory,
        // which contains an appmap.yml file.
        payload.addProperty("appmapYmlPresent", true);
        payload.add("savedFilters", gson.toJsonTree(filters));
        payload.add("mostRecentAppMaps", gson.toJsonTree(mostRecentAppMaps));
        if (codeSelection != null) {
            payload.add("codeSelection", gson.toJsonTree(codeSelection));
        }
    }

    @Override
    protected void afterInit(@Nullable Void initData) {
        var busConnection = project.getMessageBus().connect(this);
        busConnection.subscribe(AppMapSettingsListener.TOPIC, new AppMapSettingsListener() {
            @Override
            public void appMapWebViewFiltersChanged() {
                applyWebViewFilters();
            }
        });
        busConnection.subscribe(AppMapFileChangeListener.TOPIC, (AppMapFileChangeListener) changeTypes -> {
            refreshMostRecentAppMapsAlarm.cancelAndRequest();
        });
    }

    @Override
    protected void handleMessage(@NotNull String messageId, @Nullable JsonObject message) {
        if (SharedAppMapWebViewMessages.handleMessage(project, this, messageId, message)) {
            return;
        }

        switch (messageId) {
            case "open-appmap":
                var path = message != null ? message.getAsJsonPrimitive("path") : null;
                if (path != null) {
                    var appMapFile = LocalFileSystem.getInstance().findFileByNioFile(Path.of(path.getAsString()));
                    if (appMapFile != null) {
                        ApplicationManager.getApplication().invokeLater(() -> {
                            new OpenAppMapFileNavigatable(project, appMapFile, null).navigate(true);
                        }, ModalityState.defaultModalityState());
                    }
                }
                break;
            case "open-install-instructions":
                ApplicationManager.getApplication().invokeLater(() -> {
                    InstallGuideEditorProvider.open(project, InstallGuideViewPage.InstallAgent);
                }, ModalityState.defaultModalityState());
                break;
            case "open-record-instructions":
                ApplicationManager.getApplication().invokeLater(() -> {
                    InstallGuideEditorProvider.open(project, InstallGuideViewPage.RecordAppMaps);
                }, ModalityState.defaultModalityState());
                break;
            case "show-appmap-tree":
                ApplicationManager.getApplication().invokeLater(() -> {
                    AppMapToolWindowFactory.showAppMapTreePanel(project);
                }, ModalityState.defaultModalityState());
                break;
        }
    }

    protected @Nullable Void createInitData() {
        return null;
    }

    /**
     * Update the AppMap filters in the AppLand JS application.
     */
    private void applyWebViewFilters() {
        var savedFilters = AppMapProjectSettingsService.getState(project).getAppMapFilters().values();
        var message = createMessageObject("updateSavedFilters");
        message.add("data", GsonUtils.GSON.toJsonTree(savedFilters));

        postMessage(message);
    }

    /**
     * Sends the "update" notification message to the Navie webview.
     */
    private void updateMostRecentAppMaps() {
        assert (!ApplicationManager.getApplication().isDispatchThread());
        var appMapDirectory = NavieEditorProvider.KEY_APPMAP_DIRECTORY.get(file);
        if (appMapDirectory == null) {
            return;
        }

        // We need to fetch the AppMaps after indexing finished, therefore we're launching a non-blocking ReadAction in
        // a background task.
        new Task.Backgroundable(project, AppMapBundle.get("webview.navie.updatingMostRecentAppMaps"), false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                ReadAction.nonBlocking(() -> findMostRecentAppMaps(project))
                        .inSmartMode(project)
                        .wrapProgress(indicator)
                        .expireWith(NavieEditor.this)
                        .finishOnUiThread(ModalityState.defaultModalityState(), mostRecentAppMaps -> {
                            var message = createMessageObject("update");
                            message.add("mostRecentAppMaps", gson.toJsonTree(mostRecentAppMaps));
                            postMessage(message);
                        })
                        .submit(updateAppMapsThread);
            }
        }.queue();
    }

    /**
     * @return Returns the most recent AppMaps, sorted in descending order by creation time.
     * It's limited to 10 AppMaps at most.
     * The data is passed to the Navie webview as the most recent AppMaps.
     */
    @RequiresReadLock
    private static @NotNull List<AppMapListItem> findMostRecentAppMaps(@NotNull Project project) {
        ApplicationManager.getApplication().assertReadAccessAllowed();

        var timestampFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(ZoneId.systemDefault());

        return AppMapMetadataService.getInstance(project)
                .findAppMaps()
                .stream()
                .filter(appMap -> appMap.getAppMapFile() != null)
                .sorted(Comparator.comparingLong(AppMapMetadata::getModificationTimestamp).reversed())
                .limit(10)
                .map(appMap -> createListItem(appMap, timestampFormatter))
                .collect(Collectors.toList());
    }

    private static @NotNull AppMapListItem createListItem(@NotNull AppMapMetadata appMap,
                                                          @NotNull DateTimeFormatter timestampFormatter) {
        return new AppMapListItem(appMap.getName(),
                appMap.getRecorderType(),
                timestampFormatter.format(Instant.ofEpochMilli(appMap.getModificationTimestamp())),
                appMap.getSystemIndependentFilepath());
    }

    /**
     * JSON used by the Navie webview to take the most recent AppMaps.
     */
    @Value
    private static class AppMapListItem {
        @SerializedName("name")
        @NotNull String name;
        @SerializedName("recordingMethod")
        @Nullable String recordingMethod;
        @SerializedName("createdAt")
        @NotNull String createdAt;
        @SerializedName("path")
        @Nullable String path;
    }
}