package appland.webviews.appMap;

import appland.AppMapBundle;
import appland.AppMapPlugin;
import appland.files.AppMapFiles;
import appland.files.FileLocation;
import appland.files.FileLookup;
import appland.problemsView.FindingsManager;
import appland.problemsView.FindingsUtil;
import appland.problemsView.ResolvedStackLocation;
import appland.settings.AppMapApplicationSettingsService;
import appland.settings.AppMapProjectSettingsService;
import appland.settings.AppMapSettingsListener;
import appland.settings.AppMapWebViewFilter;
import appland.telemetry.TelemetryService;
import appland.upload.AppMapUploader;
import appland.utils.GsonUtils;
import appland.webviews.WebviewEditor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.intellij.ide.BrowserUtil;
import com.intellij.ide.actions.OpenInRightSplitAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.openapi.vfs.VirtualFileListener;
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.intellij.openapi.ui.Messages.showErrorDialog;

/**
 * This is similar to JetBrains' com.intellij.openapi.fileEditor.impl.HTMLFileEditor,
 * but adds additional functionality to handle the Appmap JS application.
 * <p>
 * Extending JetBrains' class isn't possible because it's internal to its module.
 */
public class AppMapFileEditor extends WebviewEditor<JsonObject> {
    private static final Logger LOG = Logger.getInstance(AppMapFileEditor.class);

    private AppMapFileEditorState webviewState;
    // keeps track if the current editor is focused
    private final AtomicBoolean isSelected = new AtomicBoolean(true);
    // keeps track if the file was modified and not yet loaded into the AppMap application
    private final AtomicBoolean isModified = new AtomicBoolean(false);

    public AppMapFileEditor(@NotNull Project project, @NotNull VirtualFile file) {
        super(project, file);
        setupVfsListener(file);
    }

    @Override
    protected @Nullable Gson createCustomizedGson() {
        return new GsonBuilder()
                .registerTypeAdapter(FileLocation.class, new FileLocation.TypeAdapter())
                .registerTypeAdapter(ResolvedStackLocation.class, new ResolvedStackLocation.TypeAdapter())
                .create();
    }

    @Override
    protected @Nullable JsonObject createInitData() {
        var fileContent = AppMapFiles.loadAppMapFile(file);

        // return early in case of error
        if (fileContent == null) {
            return null;
        }

        try {
            // parse JSON outside the ReadAction
            var appMapJson = gson.fromJson(fileContent, JsonObject.class);

            // retrieve stats from CLI and attach to the parsed AppMap
            try {
                var appMapStats = AppMapFiles.loadAppMapStats(file);
                if (appMapStats != null) {
                    var stats = GsonUtils.singlePropertyObject("functions", gson.fromJson(appMapStats, JsonArray.class));
                    appMapJson.add("stats", stats);
                }
            } catch (Exception e) {
                LOG.debug("error parsing AppMap stats", e);
            }

            // attach findings, which belong to this AppMap, as property "findings" (same as in VSCode)
            var findingsFile = ReadAction.compute(() -> AppMapFiles.findRelatedFindingsFile(file));
            if (findingsFile != null) {
                var matchingFindings = FindingsManager.getInstance(project).getAllFindings()
                        .stream()
                        .filter(problem -> findingsFile.equals(problem.getFindingsFile()))
                        .collect(Collectors.toList());
                appMapJson.add("findings", FindingsUtil.createFindingsArray(gson, project, matchingFindings, "rule"));
            }

            return appMapJson;
        } catch (Exception e) {
            LOG.warn("invalid AppMap json", e);
            return null;
        }
    }

    @Override
    protected void setupInitMessage(@Nullable JsonObject initData, @NotNull JsonObject payload) {
        var filters = AppMapProjectSettingsService.getState(project).getAppMapFilters().values();
        var props = new JsonObject();
        props.addProperty("appMapUploadable", false);
        props.addProperty("flamegraphEnabled", true);
        props.addProperty("defaultView", "viewSequence");
        props.add("savedFilters", gson.toJsonTree(filters));

        payload.add("data", initData);
        payload.add("props", props);
    }

    @Override
    protected void afterInit(@Nullable JsonObject initData) {
        if (!AppMapApplicationSettingsService.getInstance().isAppmapInstructionsViewed()) {
            AppMapApplicationSettingsService.getInstance().setAppmapInstructionsViewed(true);
            //openAppMapInstructions(); - we're not doing this until we have better instructions
        }

        project.getMessageBus().connect(this).subscribe(AppMapSettingsListener.TOPIC, new AppMapSettingsListener() {
            @Override
            public void appMapWebViewFiltersChanged() {
                applyWebViewFilters();
            }
        });

        // TODO - provide `appmap.project.language` property which specifies the language of the AppMap via metadata.language
        TelemetryService.getInstance().sendEvent("appmap:open");
    }

    @Override
    protected @NotNull Path getApplicationFile() {
        return AppMapPlugin.getAppMapHTMLPath();
    }

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) @NotNull String getName() {
        return AppMapBundle.get("appmap.editor.name");
    }

    /**
     * This method is called on the EDT
     */
    @Override
    public void selectNotify() {
        assert ApplicationManager.getApplication().isDispatchThread();

        isSelected.set(true);
        if (isModified.compareAndSet(true, false)) {
            reloadAppMapData();
        }
    }

    /**
     * This method is called on the EDT
     */
    @Override
    public void deselectNotify() {
        isSelected.set(false);
    }

    public void setWebViewState(@NotNull AppMapFileEditorState state) {
        this.webviewState = state;

        if (isWebViewReady()) {
            applyWebViewState(state);
        }
    }

    @Override
    public boolean handleMessage(@NotNull String messageId, @Nullable JsonObject message) throws Exception {
        TelemetryService telemetryService = TelemetryService.getInstance();

        switch (messageId) {
            case "webviewMounted":
                var state = webviewState;
                if (state != null) {
                    applyWebViewState(state);
                }
                return true;

            case "uploadAppMap":
                uploadAppMap();
                return true;

            case "clearSelection":
                // set empty state to the editor to restore with cleared selection
                setWebViewState(AppMapFileEditorState.EMPTY);
                return true;

            case "viewSource":
                // message is {..., location: {location:"path/file.java", externalSource="path/file.java"}}
                assert message != null;
                assert message.has("location");
                showSource(message.getAsJsonObject("location").getAsJsonPrimitive("location").getAsString());
                return true;

            case "sidebarSearchFocused":
                telemetryService.sendEvent("sidebar_search_focused");
                return true;

            case "clickFilterButton":
                telemetryService.sendEvent("click_filter_button");
                return true;

            case "clickTab":
                if (message != null) {
                    var tabId = message.getAsJsonPrimitive("tabId");
                    if (tabId.isString()) {
                        telemetryService.sendEvent("click_tab", eventData -> {
                            eventData.property("appmap.click_tab.tabId", tabId.getAsString());
                            return eventData;
                        });
                    }
                }
                return true;

            case "selectObjectInSidebar":
                if (message != null) {
                    var category = message.getAsJsonPrimitive("category");
                    if (category.isString()) {
                        telemetryService.sendEvent("select_object_in_sidebar", eventData -> {
                            eventData.property("appmap.select_object_in_sidebar.type", category.getAsString());
                            return eventData;
                        });
                    }
                }
                return true;

            case "resetDiagram":
                telemetryService.sendEvent("reset_diagram");
                return true;

            case "exportSVG":
                if (message != null) {
                    var svgString = message.getAsJsonPrimitive("svgString");
                    assert svgString.isString();
                    // choose new or existing file, write content, then open editor with the new file
                    ApplicationManager.getApplication().invokeLater(() -> {
                        ExportSvgUtil.exportToFile(project, "appMap.svg", file, svgString::getAsString, file -> {
                            new OpenFileDescriptor(project, file).navigate(true);
                        });
                    }, ModalityState.defaultModalityState());
                }
                return true;

            // filters
            case "saveFilter":
                if (message != null && message.has("filter")) {
                    var filter = gson.fromJson(message.getAsJsonObject("filter"), AppMapWebViewFilter.class);
                    AppMapProjectSettingsService.getState(project).saveAppMapWebViewFilter(filter);
                }
                return true;

            case "defaultFilter":
                if (message != null && message.has("filter")) {
                    var filter = gson.fromJson(message.getAsJsonObject("filter"), AppMapWebViewFilter.class);
                    AppMapProjectSettingsService.getState(project).saveDefaultFilter(filter);
                }
                return true;

            case "deleteFilter":
                if (message != null && message.has("filter")) {
                    var filter = gson.fromJson(message.getAsJsonObject("filter"), AppMapWebViewFilter.class);
                    AppMapProjectSettingsService.getState(project).removeAppMapWebViewFilter(filter);
                }
                return true;

            default:
                return false;
        }
    }

    @Override
    protected @NotNull String getLoadingProgressTitle() {
        return AppMapBundle.get("appmap.editor.loadingFile");
    }

    /**
     * Posts a message to the webview application with new AppMap content to load.
     */
    private void reloadAppMapData() {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            var message = createMessageObject("loadAppMap");
            message.add("data", createInitData());
            postMessage(message);
        });
    }

    private void setupVfsListener(@NotNull VirtualFile file) {
        file.getFileSystem().addVirtualFileListener(new VirtualFileListener() {
            /** This method is called on the EDT */
            @Override
            public void contentsChanged(@NotNull VirtualFileEvent event) {
                assert ApplicationManager.getApplication().isDispatchThread();
                if (!file.equals(event.getFile())) {
                    return;
                }

                if (!isWebViewReady()) {
                    LOG.debug("AppMap file was modified before webview was fully initialized");
                    return;
                }

                LOG.debug("AppMap file was modified, fromSave: " + event.isFromSave() + ", fromRefresh: " + event.isFromRefresh());
                if (isSelected.get()) {
                    // load immediately if focused
                    LOG.debug("contentsChanged: refreshing immediately");
                    reloadAppMapData();
                } else {
                    // delay if not focused
                    LOG.debug("contentsChanged: delaying refresh");
                    isModified.set(true);
                }
            }
        }, this);
    }

    /**
     * Load the current file's data into the AppLand JS application.
     */
    private void applyWebViewState(@NotNull AppMapFileEditorState state) {
        var message = createMessageObject("setAppMapState");
        message.addProperty("state", state.jsonState);
        postMessage(message);
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

    private void uploadAppMap() {
        ApplicationManager.getApplication().invokeLater(() -> {
            AppMapUploader.uploadAppMap(project, file, url -> {
                ApplicationManager.getApplication().invokeLater(() -> {
                    BrowserUtil.browse(url);
                });
            });
        }, ModalityState.defaultModalityState());
    }

    private static void showShowSourceError(@NotNull String relativePath) {
        ApplicationManager.getApplication().invokeLater(() -> {
            var title = AppMapBundle.get("appmap.editor.showSourceFileMissing.title");
            var message = AppMapBundle.get("appmap.editor.showSourceFileMissing.text", relativePath);
            showErrorDialog(message, title);
        }, ModalityState.defaultModalityState());
    }

    @RequiresBackgroundThread
    private void showSource(@NotNull String relativePath) {
        var location = FileLocation.parse(relativePath);
        if (location == null) {
            showShowSourceError(relativePath);
            return;
        }

        var referencedFile = ReadAction.compute(() -> {
            return FileLookup.findRelativeFile(project, file, FileUtil.toSystemIndependentName(location.filePath));
        });
        if (referencedFile == null) {
            showShowSourceError(relativePath);
            return;
        }

        ApplicationManager.getApplication().invokeLater(() -> {
            // IntelliJ's lines are 0-based, AppMap lines seem to be 1-based
            var descriptor = new OpenFileDescriptor(project, referencedFile, location.getZeroBasedLine(-1), -1);
            OpenInRightSplitAction.Companion.openInRightSplit(project, referencedFile, descriptor, true);
        }, ModalityState.defaultModalityState());
    }
}