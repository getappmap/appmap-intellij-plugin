package appland.webviews.findingDetails;

import appland.AppMapBundle;
import appland.AppMapPlugin;
import appland.editor.AppMapFileEditor;
import appland.editor.AppMapFileEditorState;
import appland.files.AppMapFiles;
import appland.files.FileLocation;
import appland.problemsView.ScannerProblem;
import appland.utils.GsonUtils;
import appland.webviews.WebviewEditor;
import appland.webviews.findings.FindingsOverviewEditorProvider;
import com.google.gson.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.jcef.JBCefJSQuery;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static appland.utils.GsonUtils.singlePropertyObject;

public class FindingDetailsEditor extends WebviewEditor<Void> {
    private static final int STACK_TRACE_CHARACTER_LIMIT = 50;

    public FindingDetailsEditor(@NotNull Project project, @NotNull VirtualFile file) {
        super(project, file);
    }

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) @NotNull String getName() {
        return AppMapBundle.get("webview.findingDetails.title");
    }

    @Override
    protected @NotNull Path getApplicationFile() {
        return AppMapPlugin.getFindingsAppHTMLPath();
    }

    @Override
    public @Nullable JBCefJSQuery.Response handleWebviewMessage(@NotNull String messageId, @Nullable JsonObject message) {
        switch (messageId) {
            case "open-findings-overview":
                ApplicationManager.getApplication().invokeLater(() -> {
                    FindingsOverviewEditorProvider.openEditor(project);
                }, ModalityState.defaultModalityState());
                return new JBCefJSQuery.Response("success");

            case "open-in-source-code":
                assert message != null;
                openEditorForLocation(gson.fromJson(message.getAsJsonObject("location"), ResolvedStackLocation.class));
                return new JBCefJSQuery.Response("success");

            case "open-map":
                // file path to the AppMap is at uri > path
                assert message != null;
                var path = GsonUtils.getPath(message, "uri", "path");
                if (path != null) {
                    openAppMapUri(path.getAsString());
                }
                return new JBCefJSQuery.Response("success");

            default:
                return null;
        }
    }

    @Override
    protected @Nullable Gson createCustomizedGson() {
        return new GsonBuilder()
                .registerTypeAdapter(FileLocation.class, new FileLocation.TypeAdapter())
                .registerTypeAdapter(ResolvedStackLocation.class, new ResolvedStackLocation.TypeAdapter())
                .create();
    }

    @Override
    protected @Nullable Void createInitData() {
        return null;
    }

    @Override
    protected void afterInit(@Nullable Void initData) {
        // no-op, the editor provider is handling telemetry
    }

    @Override
    protected void setupInitMessage(@Nullable Void initData, @NotNull JsonObject payload) {
        var findings = FindingDetailsEditorProvider.KEY_FINDINGS.get(file);
        assert findings != null;

        payload.addProperty("page", "finding-details");
        payload.add("data", singlePropertyObject("findings", createFindingsArray(findings)));
    }

    private void openEditorForLocation(@NotNull ResolvedStackLocation location) {
        var file = LocalFileSystem.getInstance().findFileByPath(location.absolutePath);
        if (file != null) {
            ApplicationManager.getApplication().invokeLater(() -> {
                int line = location.line != null ? location.line : 0;
                new OpenFileDescriptor(project, file, line, 0).navigate(true);
            }, ModalityState.defaultModalityState());
        } else {
            ApplicationManager.getApplication().invokeLater(() -> {
                Messages.showErrorDialog(
                        AppMapBundle.get("webview.findingDetails.locationNotFound.message"),
                        AppMapBundle.get("webview.findingDetails.locationNotFound.title"));
            });
        }
    }

    private void openAppMapUri(@NotNull String appMapUri) {
        // split into file path and state (after the "#)
        var parts = StringUtil.split(appMapUri, "#");
        var file = LocalFileSystem.getInstance().findFileByPath(parts.get(0));
        if (file != null) {
            ApplicationManager.getApplication().invokeLater(() -> {
                var appMapEditors = FileEditorManager.getInstance(project).openFile(file, true);
                if (appMapEditors.length == 1 && appMapEditors[0] instanceof AppMapFileEditor) {
                    appMapEditors[0].setState(new AppMapFileEditorState(parts.get(1)));
                }
            }, ModalityState.defaultModalityState());
        } else {
            ApplicationManager.getApplication().invokeLater(() -> {
                Messages.showErrorDialog(
                        AppMapBundle.get("webview.findingDetails.appMapNotFound.message"),
                        AppMapBundle.get("webview.findingDetails.appMapNotFound.title"));
            });
        }
    }

    private @NotNull JsonArray createFindingsArray(@NotNull List<ScannerProblem> findings) {
        var findingsJSON = new JsonArray();
        for (var finding : findings) {
            findingsJSON.add(createFindingItemJson(finding));
        }
        return findingsJSON;
    }

    private @NotNull JsonObject createFindingItemJson(@NotNull ScannerProblem finding) {
        var jsonItem = new JsonObject();
        jsonItem.add("finding", gson.toJsonTree(finding.getFinding()));
        jsonItem.add("appMapUri", createAppMapUriJson(finding));
        jsonItem.add("problemLocation", createProblemLocationJson(finding));
        jsonItem.add("stackLocations", ReadAction.compute(() -> createStackLocationsJson(finding)));
        jsonItem.add("ruleInfo", createRuleInfoJson(finding));
        jsonItem.addProperty("appMapName", findAppMapName(finding));
        return jsonItem;
    }

    // follows VSCode's "filterFinding"
    private @Nullable String findAppMapName(ScannerProblem finding) {
        var appMapFile = AppMapFiles.findAppMapSourceFile(finding.getFindingsFile());
        if (appMapFile == null) {
            return null;
        }

        var filename = appMapFile.getName();
        var index = filename.indexOf('.');
        return index == -1 ? filename : filename.substring(0, index);
    }

    private @NotNull JsonElement createProblemLocationJson(@NotNull ScannerProblem finding) {
        var location = finding.getFinding().getProblemLocation();
        if (location == null) {
            return JsonNull.INSTANCE;
        }

        var data = new JsonObject();
        data.add("range", newRangeJson(location));
        data.add("uri", singlePropertyObject("path", location.filePath));
        return data;
    }

    private @NotNull JsonElement newRangeJson(FileLocation location) {
        return location == null || location.line == null
                ? JsonNull.INSTANCE
                : singlePropertyObject("line", location.line);
    }

    private @NotNull JsonElement createStackLocationsJson(ScannerProblem finding) {
        if (finding == null) {
            return new JsonArray();
        }

        var baseFile = finding.getFindingsFile();
        var stackLocations = finding.getFinding().stack.stream()
                .map(frame -> resolveStackFrame(frame, baseFile))
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableList());

        var json = new JsonArray();
        for (var location : stackLocations) {
            json.add(gson.toJsonTree(location));
        }
        return json;
    }

    private @NotNull JsonElement createRuleInfoJson(@NotNull ScannerProblem finding) {
        return gson.toJsonTree(finding.getFinding().ruleInfo);
    }

    // follows VSCode's "resolveAppMapUri"
    private @NotNull JsonElement createAppMapUriJson(@NotNull ScannerProblem finding) {
        var appMapFile = AppMapFiles.findAppMapSourceFile(finding.getFindingsFile());
        if (appMapFile == null) {
            return JsonNull.INSTANCE;
        }

        // adds the state of the webview as URI anchor
        var state = AppMapFileEditorState
                .createViewFlowState(finding.getFinding().getEventId(), finding.getFinding().relatedEvents)
                .jsonState;
        return singlePropertyObject("path", appMapFile.toNioPath() + "#" + state);
    }

    public static @NotNull String truncatePath(@NotNull String path, @NotNull Character separator) {
        if (path.length() <= STACK_TRACE_CHARACTER_LIMIT) {
            return path;
        }

        var separatorString = "" + separator;
        while (path.contains(separatorString) && path.length() > STACK_TRACE_CHARACTER_LIMIT) {
            path = path.substring(path.indexOf(separator) + 1);
        }
        return "..." + separator + path;
    }

    private @Nullable ResolvedStackLocation resolveStackFrame(@NotNull String frame, @NotNull VirtualFile baseFile) {
        var location = FileLocation.parse(frame);
        if (location == null) {
            return null;
        }

        var resolvedFile = location.resolveFilePath(project, baseFile);
        if (resolvedFile == null || !resolvedFile.isInLocalFileSystem()) {
            return null;
        }

        var nativePath = resolvedFile.toNioPath().toString();
        var truncatedPath = truncatePath(nativePath, File.separatorChar);
        return new ResolvedStackLocation(nativePath, truncatedPath + location.getSuffix(), location.getZeroBasedLine(0));
    }
}
