package appland.webviews.appMap;

import appland.AppMapBundle;
import appland.files.AppMapFiles;
import appland.problemsView.FindingsManager;
import appland.problemsView.FindingsUtil;
import appland.settings.AppMapProjectSettingsService;
import appland.utils.GsonUtils;
import appland.webviews.webserver.AppMapWebview;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.openapi.vfs.VirtualFileListener;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * This is similar to JetBrains' com.intellij.openapi.fileEditor.impl.HTMLFileEditor,
 * but adds additional functionality to handle the Appmap JS application.
 * <p>
 * Extending JetBrains' class isn't possible because it's internal to its module.
 */
public class AppMapFileEditor extends AbstractAppMapFileView<JsonObject> {
    private static final Logger LOG = Logger.getInstance(AppMapFileEditor.class);

    // keeps track if the current editor is focused
    private final AtomicBoolean isSelected = new AtomicBoolean(true);
    // keeps track if the file was modified and not yet loaded into the AppMap application
    private final AtomicBoolean isModified = new AtomicBoolean(false);

    public AppMapFileEditor(@NotNull Project project, @NotNull VirtualFile file) {
        super(project, AppMapWebview.AppMap, file, Set.of("webviewMounted"));

        setupVfsListener(file);
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
    protected void handleMessage(@NotNull String messageId, @Nullable JsonObject message) throws Exception {
        switch (messageId) {
            case "webviewMounted":
                var state = webviewState;
                if (state != null) {
                    applyWebViewState(state);
                }
                break;
            default:
                handleAppMapBaseMessage(messageId, message);
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
}