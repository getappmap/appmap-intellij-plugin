package appland.webviews.navie;

import appland.AppMapBundle;
import appland.actions.ChooseAndAddNavieContextFilesAction;
import appland.actions.SetNavieOpenAiKeyAction;
import appland.config.AppMapConfigFileListener;
import appland.files.AppMapFiles;
import appland.files.FileLookup;
import appland.files.OpenAppMapFileNavigatable;
import appland.index.AppMapMetadata;
import appland.index.AppMapMetadataService;
import appland.index.IndexedFileListenerUtil;
import appland.installGuide.InstallGuideEditorProvider;
import appland.installGuide.InstallGuideViewPage;
import appland.notifications.AppMapNotifications;
import appland.rpcService.AppLandJsonRpcService;
import appland.settings.*;
import appland.toolwindow.AppMapToolWindowFactory;
import appland.utils.GsonUtils;
import appland.webviews.SharedAppMapWebViewMessages;
import appland.webviews.WebviewEditor;
import appland.webviews.appMap.AppMapFileEditorProvider;
import appland.webviews.appMap.AppMapFileEditorState;
import appland.webviews.webserver.AppMapWebview;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScopes;
import com.intellij.util.Alarm;
import com.intellij.util.SingleAlarm;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.concurrency.annotations.RequiresEdt;
import com.intellij.util.concurrency.annotations.RequiresReadLock;
import lombok.Value;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

public class NavieEditor extends WebviewEditor<Void> {
    private static final Logger LOG = Logger.getInstance(NavieEditor.class);

    // debounce requests to update the most recent updates by 1s
    private final SingleAlarm updateNaviePropertiesAlarm = new SingleAlarm(
            this::updateNavieProperties,
            1_000,
            this,
            Alarm.ThreadToUse.POOLED_THREAD);
    // limit updates of most recent AppMaps to a single thread for predictable ordering of the updates
    private final Executor updateAppMapsThread = AppExecutorUtil.createBoundedApplicationPoolExecutor("Navie AppMap updater", 1);

    public NavieEditor(@NotNull Project project, @NotNull VirtualFile file) {
        super(project, AppMapWebview.Navie, file, SharedAppMapWebViewMessages.withBaseMessages(
                "choose-files-to-pin",
                "open-appmap",
                "open-install-instructions",
                "open-location",
                "open-new-chat",
                "open-record-instructions",
                "select-llm-option",
                "show-appmap-tree"));
    }

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) @NotNull String getName() {
        return AppMapBundle.get("webview.navie.title");
    }

    @Override
    protected void setupInitMessage(@Nullable Void initData, @NotNull JsonObject payload) {
        AppMapApplicationSettings settings = AppMapApplicationSettingsService.getInstance();
        var apiKey = settings.getApiKey();
        var useAnimation = settings.isUseAnimation();
        var filters = AppMapProjectSettingsService.getState(project).getAppMapFilters().values();
        var codeSelection = NavieEditorProvider.KEY_CODE_SELECTION.get(file);
        var promptSuggestion = NavieEditorProvider.KEY_PROMPT_SUGGESTION.get(file);

        var port = NavieEditorProvider.KEY_INDEXER_RPC_PORT.get(file);
        assert port != null;

        var updatableNavieData = DumbService.getInstance(project).runReadActionInSmartMode(() -> {
            return createUpdatableNavieData(project);
        });

        var appMapContextFile = NavieEditorProvider.KEY_APPMAP_CONTEXT_FILE.get(file);
        if (appMapContextFile != null) {
            var fileNioPath = appMapContextFile.getFileSystem().getNioPath(appMapContextFile);
            assert fileNioPath != null;
            payload.addProperty("targetAppmapFsPath", fileNioPath.toString());

            var fileContent = AppMapFiles.loadAppMapFile(appMapContextFile);
            payload.add("targetAppmapData", gson.fromJson(fileContent, JsonObject.class));
        }

        payload.addProperty("appmapRpcPort", port);
        payload.addProperty("apiKey", StringUtil.defaultIfEmpty(apiKey, ""));
        payload.addProperty("appmapYmlPresent", updatableNavieData.isAppMapConfigPresent);
        payload.add("savedFilters", gson.toJsonTree(filters));
        payload.add("mostRecentAppMaps", gson.toJsonTree(updatableNavieData.mostRecentAppMaps));
        if (codeSelection != null) {
            payload.add("codeSelection", gson.toJsonTree(codeSelection));
        }
        if (promptSuggestion != null) {
            payload.add("suggestion", gson.toJsonTree(promptSuggestion));
        }
        payload.addProperty("useAnimation", useAnimation);
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
        busConnection.subscribe(AppMapConfigFileListener.TOPIC, (AppMapConfigFileListener) updateNaviePropertiesAlarm::cancelAndRequest);

        // listen for changes of AppMap files
        IndexedFileListenerUtil.registerListeners(project, this, true, false, false, updateNaviePropertiesAlarm::cancelAndRequest);
    }

    @Override
    protected void handleMessage(@NotNull String messageId, @Nullable JsonObject message) {
        if (SharedAppMapWebViewMessages.handleMessage(project, this, messageId, message)) {
            return;
        }

        switch (messageId) {
            case "choose-files-to-pin":
                handleChooseFilesToPin();
                break;
            case "open-appmap": {
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
            }
            case "open-install-instructions":
                ApplicationManager.getApplication().invokeLater(() -> {
                    InstallGuideEditorProvider.open(project, InstallGuideViewPage.InstallAgent);
                }, ModalityState.defaultModalityState());
                break;
            case "open-location": {
                var path = message != null ? message.getAsJsonPrimitive("path") : null;
                var directory = message != null ? message.getAsJsonPrimitive("directory") : null;
                if (path != null) {
                    handleOpenLocation(path.getAsString(), directory != null ? directory.getAsString() : null);
                }
                break;
            }
            case "open-new-chat":
                ApplicationManager.getApplication().invokeLater(() -> {
                    NavieEditorProvider.openEditor(project, DataContext.EMPTY_CONTEXT);
                }, ModalityState.defaultModalityState());
                break;
            case "open-record-instructions":
                ApplicationManager.getApplication().invokeLater(() -> {
                    InstallGuideEditorProvider.open(project, InstallGuideViewPage.RecordAppMaps);
                }, ModalityState.defaultModalityState());
                break;
            case "select-llm-option":
                var choice = message != null ? message.get("choice") : null;
                if (choice != null) {
                    handleSelectLlmOption(choice.getAsString());
                }
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
     * Sends the "update" notification message to the Navie webview
     * with properties 'appmapYmlPresent' and 'mostRecentAppMaps'.
     */
    private void updateNavieProperties() {
        assert (!ApplicationManager.getApplication().isDispatchThread());

        // We need to fetch the AppMaps after indexing finished, therefore we're launching a non-blocking ReadAction in
        // a background task.
        new Task.Backgroundable(project, AppMapBundle.get("webview.navie.updatingMostRecentAppMaps"), false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                ReadAction.nonBlocking(() -> createUpdatableNavieData(project))
                        .inSmartMode(project)
                        .wrapProgress(indicator)
                        .expireWith(NavieEditor.this)
                        .finishOnUiThread(ModalityState.defaultModalityState(), navieData -> {
                            var message = createMessageObject("update");
                            message.add("appmapYmlPresent", gson.toJsonTree(navieData.isAppMapConfigPresent));
                            message.add("mostRecentAppMaps", gson.toJsonTree(navieData.mostRecentAppMaps));
                            postMessage(message);
                        })
                        .submit(updateAppMapsThread);
            }
        }.queue();
    }

    private void handleChooseFilesToPin() {
        ApplicationManager.getApplication().invokeLater(() -> {
            ChooseAndAddNavieContextFilesAction.chooseAndAddPinnedFiles(project, this);
        });
    }

    private void handleOpenLocation(@NotNull String pathWithLineRange, @Nullable String directory) {
        var colonIndex = pathWithLineRange.lastIndexOf(':');
        var filePath = colonIndex > 0 ? pathWithLineRange.substring(0, colonIndex) : pathWithLineRange;
        var lineRangeOrEventId = colonIndex > 0 ? pathWithLineRange.substring(colonIndex + 1) : null;
        var searchScope = directory != null ? createDirectorySearchScope(project, directory) : null;

        var virtualFile = ReadAction.compute(() -> FileLookup.findRelativeFile(project, searchScope, null, filePath));
        if (virtualFile == null) {
            ApplicationManager.getApplication().invokeLater(() -> {
                Messages.showErrorDialog(
                        project,
                        AppMapBundle.get("notification.genericFileNotFound.message"),
                        AppMapBundle.get("notification.genericFileNotFound.title"));
            });
            return;
        }

        if (AppMapFiles.isAppMapFileName(filePath)) {
            // If the path is an appmap.json, the starting line is actually an event ID.
            var state = AppMapFileEditorState.createViewSequence(lineRangeOrEventId);
            ApplicationManager.getApplication().invokeLater(() -> {
                AppMapFileEditorProvider.openAppMap(project, virtualFile, state);
            }, ModalityState.defaultModalityState());
        } else {
            // Other files should be opened as text files.
            // Lines:
            // null: no line,
            // size 1: just the start line,
            // size 2: start and end line,
            // other sizes are not possible.
            var lineRange = lineRangeOrEventId != null ? mapToLineRange(lineRangeOrEventId) : null;
            ApplicationManager.getApplication().invokeLater(() -> {
                var startLine = lineRange != null ? lineRange[0] : 0;
                // scrolls to the start line, the API does not support scrolling to a range
                new OpenFileDescriptor(project, virtualFile, startLine, 0).navigate(true);
            }, ModalityState.defaultModalityState());
        }
    }

    private void handleSelectLlmOption(@NotNull String choice) {
        switch (choice) {
            // Clear LLM environment settings and remove OpenAPI key
            case "default": {
                var environment = new HashMap<>(AppMapApplicationSettingsService.getInstance().getCliEnvironment());
                for (var dropped : AppLandJsonRpcService.LLM_ENV_VARIABLES) {
                    environment.remove(dropped);
                }
                AppMapApplicationSettingsService.getInstance().setCliEnvironment(environment);
                AppMapSecureApplicationSettingsService.getInstance().setOpenAIKey(null);
                return;
            }
            // Ask user for OpenAI API key
            case "own-key": {
                ApplicationManager.getApplication().invokeLater(() -> {
                    SetNavieOpenAiKeyAction.showInputDialog(project);
                }, ModalityState.defaultModalityState());
                return;
            }
            // Unused, the webview opens a browser windowK
            case "own-model":
                return;
            default:
                LOG.warn("Unknown type passed to select-llm-option: " + choice);
        }
    }

    /**
     * @param directoryPath Native OS directory path as a string.
     * @return A search scope which is restricted to the given directory. {@code null} is returned if the path could not be found.
     */
    private static @Nullable GlobalSearchScope createDirectorySearchScope(@NotNull Project project,
                                                                          @NotNull String directoryPath) {
        try {
            var directory = LocalFileSystem.getInstance().findFileByNioFile(Path.of(directoryPath));
            if (directory != null) {
                return GlobalSearchScopes.directoryScope(project, directory, true);
            }
        } catch (InvalidPathException e) {
            // ignore
        }
        return null;
    }

    /**
     * Maps a string with "n" or "n-m" to an int array containing the available numbers.
     *
     * @param lineRange A single number or range of numbers "n-m"
     * @return Array of start and end line, just the start line or {@code null} if the input is invalid.
     * The lines are adjusted from 1-based to 0-based to match the JetBrains API.
     */
    private static int @Nullable [] mapToLineRange(@NotNull String lineRange) {
        var parts = lineRange.split("-");
        if (parts.length == 2) {
            return new int[]{Integer.parseInt(parts[0]) - 1, Integer.parseInt(parts[1]) - 1};
        }
        if (parts.length == 1) {
            return new int[]{Integer.parseInt(parts[0]) - 1};
        }
        return null;
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
     * Creates the updatable Navie data in a single read-action.
     *
     * @param project Project
     * @return Data, which can change while the Navie editor is open.
     */
    @RequiresReadLock
    private static @NotNull UpdatableNavieData createUpdatableNavieData(@NotNull Project project) {
        var isAppMapYamlPresent = AppMapFiles.isAppMapConfigAvailable(project);
        var mostRecentAppMaps = findMostRecentAppMaps(project);
        return new UpdatableNavieData(isAppMapYamlPresent, mostRecentAppMaps);
    }

    /**
     * Adds the given files as pinned files to the Navie webview.
     * <p>
     * If a file is larger than the configured max file size, then it's excluded and a notification is shown to the user.
     *
     * @param contextFiles Files to pin in the webview
     */
    @RequiresEdt
    public void addPinnedFiles(@NotNull List<VirtualFile> contextFiles) {
        if (contextFiles.isEmpty()) {
            return;
        }

        var maxPinnedFileSizeKB = AppMapApplicationSettingsService.getInstance().getMaxPinnedFileSizeKB();

        var validFileRequests = ReadAction.compute(() -> {
            var validRequests = new ArrayList<NaviePinFileRequest>();
            for (var file : contextFiles) {
                var isValid = file.isValid()
                        && file.isInLocalFileSystem()
                        && !file.isDirectory()
                        && file.getLength() <= 1024L * maxPinnedFileSizeKB;

                if (isValid) {
                    var document = FileDocumentManager.getInstance().getDocument(file);
                    if (document != null) {
                        var url = file.getUrl();
                        validRequests.add(new NaviePinFileRequest(file.getPresentableName(), url, document.getText()));
                    }
                }
            }
            return validRequests;
        });

        if (!validFileRequests.isEmpty()) {
            var pinFiles = createMessageObject("pin-files");
            pinFiles.add("requests", gson.toJsonTree(validFileRequests));
            postMessage(pinFiles);
        }

        var tooLargeFilesCount = contextFiles.size() - validFileRequests.size();
        if (tooLargeFilesCount > 0) {
            AppMapNotifications.showNaviePinnedFileTooLargeNotification(project, tooLargeFilesCount, maxPinnedFileSizeKB);
        }
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

    @Value
    private static class UpdatableNavieData {
        boolean isAppMapConfigPresent;
        List<AppMapListItem> mostRecentAppMaps;
    }
}
