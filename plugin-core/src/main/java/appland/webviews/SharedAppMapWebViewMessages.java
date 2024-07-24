package appland.webviews;

import appland.AppMapBundle;
import appland.actions.OpenInRightSplit;
import appland.files.FileLocation;
import appland.files.FileLookup;
import appland.notifications.AppMapNotifications;
import appland.settings.AppMapProjectSettingsService;
import appland.settings.AppMapWebViewFilter;
import appland.telemetry.TelemetryService;
import appland.utils.GsonUtils;
import appland.webviews.appMap.ExportSvgUtil;
import com.google.gson.JsonObject;
import com.intellij.ide.actions.RevealFileAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static com.intellij.openapi.ui.Messages.showErrorDialog;

/**
 * Handling of common {@link WebviewEditor} messages for AppMap views, i.e. for the AppMap editor and the
 * AppMap view embedded in the {@link appland.webviews.navie.NavieEditor} webview.
 */
public final class SharedAppMapWebViewMessages {
    private SharedAppMapWebViewMessages() {
    }

    private static final Logger LOG = Logger.getInstance(SharedAppMapWebViewMessages.class);
    private final static Set<String> BASE_MESSAGES = Set.of(
            "clearSelection",
            "clickFilterButton",
            "clickTab",
            "defaultFilter",
            "deleteFilter",
            "exportJSON",
            "exportSVG",
            "resetDiagram",
            "saveFilter",
            "selectObjectInSidebar",
            "sidebarSearchFocused",
            "viewSource");

    /**
     * @param additionalMessages Additional messages handled by the calling WebView editor
     * @return Basic messages support by this class and additional messages supported by the {@link WebviewEditor} calling this method.
     */
    public static @NotNull Set<String> withBaseMessages(@NotNull String... additionalMessages) {
        var result = new HashSet<>(BASE_MESSAGES);
        Collections.addAll(result, additionalMessages);
        return result;
    }

    /**
     * Handle the given webview message.
     *
     * @param project   Project
     * @param editor    WebView editor
     * @param messageId Message to handle
     * @param message   Message payload
     * @return {@code true} if this message was successfully handled by this method. {@code false} indicates that the caller is supposed to handle it afterward.
     */
    public static boolean handleMessage(@NotNull Project project,
                                        @NotNull WebviewEditor<?> editor,
                                        @NotNull String messageId,
                                        @Nullable JsonObject message) {
        if (!BASE_MESSAGES.contains(messageId)) {
            return false;
        }

        var telemetryService = TelemetryService.getInstance();
        var gson = editor.gson;
        switch (messageId) {
            case "clearSelection":
                // set empty state to the editor to restore with cleared selection
                editor.clearState();
                return true;

            case "viewSource":
                // message is {..., location: {location:"path/file.java", externalSource="path/file.java"}}
                assert message != null;
                assert message.has("location");
                showSource(project, editor, message.getAsJsonObject("location").getAsJsonPrimitive("location").getAsString());
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
                        telemetryService.sendEvent("click_tab", eventData -> {
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
                    exportSVG(project, editor, message);
                }
                return true;

            case "exportJSON":
                if (message != null) {
                    exportJSON(project, message);
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
                LOG.debug("Unexpected AppMap webview message: " + messageId);
                return false;
        }
    }

    @RequiresBackgroundThread
    private static void showSource(@NotNull Project project,
                                   @NotNull WebviewEditor<?> editor,
                                   @NotNull String relativePath) {
        var location = FileLocation.parse(relativePath);
        if (location == null) {
            showShowSourceError(relativePath);
            return;
        }

        var referencedFile = ReadAction.compute(() -> {
            return FileLookup.findRelativeFile(project, editor.getFile(), FileUtil.toSystemIndependentName(location.filePath));
        });
        if (referencedFile == null) {
            showShowSourceError(relativePath);
            return;
        }

        ApplicationManager.getApplication().invokeLater(() -> {
            // IntelliJ's lines are 0-based, AppMap lines seem to be 1-based
            var descriptor = new OpenFileDescriptor(project, referencedFile, location.getZeroBasedLine(-1), -1);
            OpenInRightSplit.openInRightSplit(project, referencedFile, descriptor);
        }, ModalityState.defaultModalityState());
    }

    private static void showShowSourceError(@NotNull String relativePath) {
        ApplicationManager.getApplication().invokeLater(() -> {
            var title = AppMapBundle.get("appmap.editor.showSourceFileMissing.title");
            var message = AppMapBundle.get("appmap.editor.showSourceFileMissing.text", relativePath);
            showErrorDialog(message, title);
        }, ModalityState.defaultModalityState());
    }

    private static void exportSVG(@NotNull Project project, @NotNull WebviewEditor<?> editor, @NotNull JsonObject message) {
        var svgString = message.getAsJsonPrimitive("svgString");
        assert svgString.isString();
        // choose new or existing file, write content, then open editor with the new file
        ApplicationManager.getApplication().invokeLater(() -> {
            ExportSvgUtil.exportToFile(project, "appMap.svg", editor.getFile(), svgString::getAsString, file -> {
                new OpenFileDescriptor(project, file).navigate(true);
            });
        }, ModalityState.defaultModalityState());
    }

    private static void exportJSON(@NotNull Project project, @NotNull JsonObject message) {
        if (!message.has("appmapData")) {
            return;
        }

        var appMapData = message.getAsJsonObject("appmapData");
        var metadata = appMapData.has("metadata") ? appMapData.getAsJsonObject("metadata") : null;
        var basename = metadata != null && metadata.has("name")
                ? metadata.getAsJsonPrimitive("name").getAsString()
                : createRandomString(16);
        try {
            var tempFile = FileUtil.createTempFile(basename.replaceAll("[^a-zA-Z0-9\\-_ ]", "_"), ".appmap.json", false);
            FileUtil.writeToFile(tempFile, GsonUtils.GSON.toJson(appMapData));
            RevealFileAction.openFile(tempFile);
        } catch (IOException e) {
            LOG.debug("Exception creating or writing to AppMap JSON file", e);
            AppMapNotifications.showAppMapJsonExportFailedNotification(project, e.getLocalizedMessage());
        }
    }

    private static @NotNull String createRandomString(int length) {
        var buffer = new byte[length];
        new Random().nextBytes(buffer);
        return StringUtil.toHexString(buffer);
    }
}
