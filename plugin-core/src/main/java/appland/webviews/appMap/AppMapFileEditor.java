package appland.webviews.appMap;

import appland.AppMapBundle;
import appland.files.AppMapFiles;
import appland.files.FileLocation;
import appland.problemsView.FindingsManager;
import appland.problemsView.FindingsUtil;
import appland.problemsView.ResolvedStackLocation;
import appland.settings.AppMapProjectSettingsService;
import appland.settings.AppMapSettingsListener;
import appland.utils.GsonUtils;
import appland.webviews.SharedAppMapWebViewMessages;
import appland.webviews.WebviewEditor;
import appland.webviews.navie.NavieEditorProvider;
import appland.webviews.webserver.AppMapWebview;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.openapi.vfs.VirtualFileListener;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

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
        super(project, AppMapWebview.AppMap, file, SharedAppMapWebViewMessages.withBaseMessages(
                "ask-navie-about-map",
                "webviewMounted")
        );
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
        var fileNioPath = file.getFileSystem().getNioPath(file);
        var filters = AppMapProjectSettingsService.getState(project).getAppMapFilters().values();

        var props = new JsonObject();
        props.addProperty("appMapUploadable", false);
        props.addProperty("flamegraphEnabled", true);
        props.addProperty("defaultView", "viewSequence");
        props.add("savedFilters", gson.toJsonTree(filters));
        props.addProperty("appmapFsPath", fileNioPath != null ? fileNioPath.normalize().toString() : null);

        payload.add("data", initData);
        payload.add("props", props);
    }

    @Override
    protected void afterInit(@Nullable JsonObject initData) {
        project.getMessageBus().connect(this).subscribe(AppMapSettingsListener.TOPIC, new AppMapSettingsListener() {
            @Override
            public void appMapWebViewFiltersChanged() {
                applyWebViewFilters();
            }
        });
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

    @Override
    public void clearState() {
        setWebViewState(AppMapFileEditorState.EMPTY);
    }

    public void setWebViewState(@NotNull AppMapFileEditorState state) {
        this.webviewState = state;

        if (isWebViewReady()) {
            applyWebViewState(state);
        }
    }

    @Override
    protected void handleMessage(@NotNull String messageId, @Nullable JsonObject message) {
        if (SharedAppMapWebViewMessages.handleMessage(project, this, messageId, message)) {
            return;
        }

        switch (messageId) {
            case "ask-navie-about-map":
                var nativeAppMapPath = message != null && message.has("mapFsPath")
                        ? message.getAsJsonPrimitive("mapFsPath").getAsString()
                        : null;
                if (nativeAppMapPath != null) {
                    var appMapFile = LocalFileSystem.getInstance().findFileByNioFile(Path.of(nativeAppMapPath));
                    if (appMapFile != null) {
                        ApplicationManager.getApplication().invokeLater(() -> {
                            NavieEditorProvider.openEditorForAppMap(project, appMapFile);
                        }, ModalityState.defaultModalityState());
                        return;
                    }
                }
                // fallback if mapFsPath was null or the file wasn't found
                LOG.debug("Unable to locate AppMap file for message " + messageId + ": " + message);
                break;
            case "webviewMounted":
                var state = webviewState;
                if (state != null) {
                    applyWebViewState(state);
                }
                break;
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
}