package appland.webviews.appMap;

import appland.AppMapBundle;
import appland.files.FileLocation;
import appland.files.FileLookup;
import appland.problemsView.ResolvedStackLocation;
import appland.settings.AppMapProjectSettingsService;
import appland.settings.AppMapSettingsListener;
import appland.settings.AppMapWebViewFilter;
import appland.telemetry.TelemetryService;
import appland.upload.AppMapUploader;
import appland.utils.GsonUtils;
import appland.webviews.WebviewEditor;
import appland.webviews.webserver.AppMapWebview;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.intellij.ide.BrowserUtil;
import com.intellij.ide.actions.OpenInRightSplitAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

import static com.intellij.openapi.ui.Messages.showErrorDialog;

/**
 * Base class for webview editors, which are able to render Appmap files.
 *
 * @see appland.webviews.appMap.AppMapFileEditor
 * @see NavieEditor
 */
abstract class AbstractAppMapFileView<T> extends WebviewEditor<T> {
    // messages defined in the shared JavaScript handler function handleAppMapMessages(...).
    private static final Set<String> APPMAP_MESSAGES = Set.of(
            "viewSource", "clearSelection", "uploadAppMap", "sidebarSearchFocused", "clickFilterButton",
            "clickTab", "selectObjectInSidebar", "resetDiagram", "exportSVG", "request-resolve-location",
            "saveFilter", "deleteFilter", "defaultFilter"
    );

    protected @Nullable AppMapFileEditorState webviewState;

    protected AbstractAppMapFileView(@NotNull Project project,
                                     @NotNull AppMapWebview webview,
                                     @NotNull VirtualFile file) {
        this(project, webview, file, Set.of());
    }

    protected AbstractAppMapFileView(@NotNull Project project,
                                     @NotNull AppMapWebview webview,
                                     @NotNull VirtualFile file,
                                     @NotNull Set<String> additionallySupportedMessages) {
        super(project, webview, file, createSupportedMessages(additionallySupportedMessages));
    }

    @Override
    protected @Nullable Gson createCustomizedGson() {
        return new GsonBuilder()
                .registerTypeAdapter(FileLocation.class, new FileLocation.TypeAdapter())
                .registerTypeAdapter(ResolvedStackLocation.class, new ResolvedStackLocation.TypeAdapter())
                .create();
    }

    @Override
    protected void afterInit(@Nullable T initData) {
        project.getMessageBus().connect(this).subscribe(AppMapSettingsListener.TOPIC, new AppMapSettingsListener() {
            @Override
            public void appMapWebViewFiltersChanged() {
                applyWebViewFilters();
            }
        });
    }

    public void setWebViewState(@NotNull AppMapFileEditorState state) {
        this.webviewState = state;

        if (isWebViewReady()) {
            applyWebViewState(state);
        }
    }

    /**
     * Handles the given message if it's supported.
     *
     * @param messageId ID of the message
     * @param message   Message payload
     * @return If the message was successfully handled.
     */
    protected boolean handleAppMapBaseMessage(@NotNull String messageId, @Nullable JsonObject message) throws Exception {
        switch (messageId) {
            case "viewSource":
                // message is {..., location: {location:"path/file.java", externalSource="path/file.java"}}
                assert message != null;
                assert message.has("location");
                handleViewSource(message.getAsJsonObject("location").getAsJsonPrimitive("location").getAsString());
                return true;

            case "clearSelection":
                // set empty state to the editor to restore with cleared selection
                setWebViewState(AppMapFileEditorState.EMPTY);
                return true;

            case "uploadAppMap":
                handleUploadAppMap();
                return true;

            // known message, but not handled
            case "sidebarSearchFocused":
                return true;

            // known message, but not handled
            case "clickFilterButton":
                return true;

            case "clickTab":
                if (message != null) {
                    var tabId = message.getAsJsonPrimitive("tabId");
                    if (tabId.isString()) {
                        TelemetryService.getInstance().sendEvent("click_tab", eventData -> {
                            eventData.property("appmap.click_tab.tabId", tabId.getAsString());
                            return eventData;
                        });
                    }
                }
                return true;

            // known message, but not handled
            case "selectObjectInSidebar":
                return true;

            // known message, but not handled
            case "resetDiagram":
                return true;

            case "exportSVG":
                if (message != null) {
                    handleExportSvg(message);
                }
                return true;

            case "saveFilter":
                if (message != null && message.has("filter")) {
                    handleSaveFilter(message);
                }
                return true;

            case "defaultFilter":
                if (message != null && message.has("filter")) {
                    handleDefaultFilter(message);
                }
                return true;

            case "deleteFilter":
                if (message != null && message.has("filter")) {
                    handleDeleteFilter(message);
                }
                return true;
        }

        return false;
    }

    @RequiresBackgroundThread
    protected void handleViewSource(@NotNull String relativePath) {
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

    /**
     * Update the AppMap filters in the AppLand JS application.
     */
    protected void applyWebViewFilters() {
        var savedFilters = AppMapProjectSettingsService.getState(project).getAppMapFilters().values();
        var message = createMessageObject("updateSavedFilters");
        message.add("data", GsonUtils.GSON.toJsonTree(savedFilters));
        postMessage(message);
    }

    private void handleUploadAppMap() {
        ApplicationManager.getApplication().invokeLater(() -> {
            AppMapUploader.uploadAppMap(project, file, url -> {
                ApplicationManager.getApplication().invokeLater(() -> {
                    BrowserUtil.browse(url);
                });
            });
        }, ModalityState.defaultModalityState());
    }

    protected void handleExportSvg(@NotNull JsonObject message) {
        var svgString = message.getAsJsonPrimitive("svgString");
        assert svgString.isString();
        // choose new or existing file, write content, then open editor with the new file
        ApplicationManager.getApplication().invokeLater(() -> {
            ExportSvgUtil.exportToFile(project, "appMap.svg", file, svgString::getAsString, file -> {
                new OpenFileDescriptor(project, file).navigate(true);
            });
        }, ModalityState.defaultModalityState());
    }

    protected void handleSaveFilter(@NotNull JsonObject message) {
        var filter = gson.fromJson(message.getAsJsonObject("filter"), AppMapWebViewFilter.class);
        AppMapProjectSettingsService.getState(project).saveAppMapWebViewFilter(filter);
    }

    protected void handleDeleteFilter(@NotNull JsonObject message) {
        var filter = gson.fromJson(message.getAsJsonObject("filter"), AppMapWebViewFilter.class);
        AppMapProjectSettingsService.getState(project).removeAppMapWebViewFilter(filter);
    }

    protected void handleDefaultFilter(@NotNull JsonObject message) {
        var filter = gson.fromJson(message.getAsJsonObject("filter"), AppMapWebViewFilter.class);
        AppMapProjectSettingsService.getState(project).saveDefaultFilter(filter);
    }

    /**
     * Load the current file's data into the AppLand JS application.
     */
    protected void applyWebViewState(@NotNull AppMapFileEditorState state) {
        var message = createMessageObject("setAppMapState");
        message.addProperty("state", state.jsonState);
        postMessage(message);
    }

    private static void showShowSourceError(@NotNull String relativePath) {
        ApplicationManager.getApplication().invokeLater(() -> {
            var title = AppMapBundle.get("appmap.editor.showSourceFileMissing.title");
            var message = AppMapBundle.get("appmap.editor.showSourceFileMissing.text", relativePath);
            showErrorDialog(message, title);
        }, ModalityState.defaultModalityState());
    }

    private static Set<String> createSupportedMessages(@NotNull Set<String> additionalMessages) {
        if (additionalMessages.isEmpty()) {
            return AbstractAppMapFileView.APPMAP_MESSAGES;
        }

        var result = new HashSet<>(AbstractAppMapFileView.APPMAP_MESSAGES);
        result.addAll(additionalMessages);
        return result;
    }
}
