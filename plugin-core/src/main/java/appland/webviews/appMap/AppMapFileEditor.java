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
import appland.telemetry.TelemetryService;
import appland.upload.AppMapUploader;
import appland.webviews.WebviewEditor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.intellij.ide.BrowserUtil;
import com.intellij.ide.actions.OpenInRightSplitAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.fileEditor.FileEditorStateLevel;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.openapi.vfs.VirtualFileListener;
import com.intellij.ui.jcef.JBCefJSQuery;
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

    private FileEditorState state;
    // keeps track if the current editor is focused
    private final AtomicBoolean isSelected = new AtomicBoolean(true);
    // keeps track if the file was modified and not yet loaded into the AppMap application
    private final AtomicBoolean isModified = new AtomicBoolean(false);

    public AppMapFileEditor(Project project, VirtualFile file) {
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

    /**
     * @return The JSON object to pass to the AppMap webview as "data" property.
     */
    @Override
    protected @Nullable JsonObject createInitData() {
        var fileContent = ReadAction.compute(() -> {
            var document = FileDocumentManager.getInstance().getDocument(file);
            if (document == null) {
                LOG.error("unable to retrieve document for file: " + file.getPath());
                return null;
            }

            return document.getText();
        });

        // return early in case of error
        if (fileContent == null) {
            return null;
        }

        try {
            // parse JSON outside the ReadAction
            var appMapJson = gson.fromJson(fileContent, JsonObject.class);

            // attach findings, which belong to this AppMap, as property "findings" (same as in VSCode)
            var findingsFile = ReadAction.compute(() -> AppMapFiles.findRelatedFindingsFile(file));
            if (findingsFile != null) {
                var matchingFindings = FindingsManager.getInstance(project).getAllProblems()
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
        payload.add("data", initData);
    }

    @Override
    protected void afterInit(@Nullable JsonObject initData) {
        if (!AppMapApplicationSettingsService.getInstance().isAppmapInstructionsViewed()) {
            AppMapApplicationSettingsService.getInstance().setAppmapInstructionsViewed(true);
            //openAppMapInstructions(); - we're not doing this until we have better instructions
        }

        // TODO - provide `appmap.project.language` property which specifies the language of the AppMap via metadata.language
        TelemetryService.getInstance().sendEvent("appmap:open");

        var state = getState(FileEditorStateLevel.FULL);
        if (state instanceof AppMapFileEditorState) {
            applyEditorState((AppMapFileEditorState) state);
        }
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

    @Override
    public @NotNull FileEditorState getState(@NotNull FileEditorStateLevel level) {
        var current = this.state;
        return current == null ? FileEditorState.INSTANCE : current;
    }

    @Override
    public void setState(@NotNull FileEditorState state) {
        this.state = state;

        if (isWebViewReady() && state instanceof AppMapFileEditorState) {
            applyEditorState((AppMapFileEditorState) state);
        }
    }

    @Override
    public @Nullable JBCefJSQuery.Response handleWebviewMessage(@NotNull String messageId, @Nullable JsonObject message) {
        TelemetryService telemetryService = TelemetryService.getInstance();

        switch (messageId) {
            case "uploadAppMap":
                return uploadAppMap();

            case "viewSource":
                // message is {..., location: {location:"path/file.java", externalSource="path/file.java"}}
                if (message != null) {
                    var location = message.getAsJsonObject("location");
                    if (location != null) {
                        return showSource(location.getAsJsonPrimitive("location").getAsString());
                    }
                }
                return null;

            case "sidebarSearchFocused":
                telemetryService.sendEvent("sidebar_search_focused");
                return null;

            case "clickFilterButton":
                telemetryService.sendEvent("click_filter_button");
                return null;

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
                return null;

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
                return null;

            case "resetDiagram":
                telemetryService.sendEvent("reset_diagram");
                return null;

            default:
                return null;
        }
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
    private void applyEditorState(@NotNull AppMapFileEditorState state) {
        var message = createMessageObject("setAppMapState");
        message.addProperty("state", state.jsonState);
        postMessage(message);
    }

    @Nullable
    private JBCefJSQuery.Response uploadAppMap() {
        ApplicationManager.getApplication().invokeLater(() -> {
            AppMapUploader.uploadAppMap(project, file, url -> {
                ApplicationManager.getApplication().invokeLater(() -> {
                    BrowserUtil.browse(url);
                });
            });
        }, ModalityState.defaultModalityState());
        return null;
    }

    private static void showShowSourceError(@NotNull String relativePath) {
        ApplicationManager.getApplication().invokeLater(() -> {
            var title = AppMapBundle.get("appmap.editor.showSourceFileMissing.title");
            var message = AppMapBundle.get("appmap.editor.showSourceFileMissing.text", relativePath);
            showErrorDialog(message, title);
        }, ModalityState.defaultModalityState());
    }

    @RequiresBackgroundThread
    private @Nullable JBCefJSQuery.Response showSource(@NotNull String relativePath) {
        var location = FileLocation.parse(relativePath);
        if (location == null) {
            showShowSourceError(relativePath);
            return null;
        }

        var referencedFile = ReadAction.compute(() -> {
            return FileLookup.findRelativeFile(project, file, FileUtil.toSystemIndependentName(location.filePath));
        });
        if (referencedFile == null) {
            showShowSourceError(relativePath);
            return null;
        }

        ApplicationManager.getApplication().invokeLater(() -> {
            // IntelliJ's lines are 0-based, AppMap lines seem to be 1-based
            var descriptor = new OpenFileDescriptor(project, referencedFile, location.getZeroBasedLine(-1), -1);
            OpenInRightSplitAction.Companion.openInRightSplit(project, referencedFile, descriptor, true);
        }, ModalityState.defaultModalityState());
        return null;
    }
}