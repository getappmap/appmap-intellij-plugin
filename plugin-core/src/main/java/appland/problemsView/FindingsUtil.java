package appland.problemsView;

import appland.files.AppMapFiles;
import appland.files.FileLocation;
import appland.problemsView.model.ScannerFinding;
import appland.webviews.appMap.AppMapFileEditorState;
import com.google.gson.*;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static appland.utils.GsonUtils.singlePropertyObject;

public final class FindingsUtil {
    private static final int STACK_TRACE_CHARACTER_LIMIT = 50;

    private FindingsUtil() {
    }

    /**
     * Creates an array of findings in the JSON format expected by the "Findings details" webview and
     * the "AppMap" webview.
     *
     * @param gson             To read and write JSON
     * @param project          Current project
     * @param findings         Findings to transform
     * @param rulePropertyName Name of the rule property of the findings items.
     *                         The AppMap webview needs a different property name than the findings details webview.
     * @return JSON array, will be empty for an empty list of findings
     */
    public static @NotNull JsonArray createFindingsArray(@NotNull Gson gson,
                                                         @NotNull Project project,
                                                         @NotNull List<ScannerFinding> findings,
                                                         @NotNull String rulePropertyName) {
        var findingsJSON = new JsonArray();
        for (var finding : findings) {
            findingsJSON.add(createFindingItemJson(gson, project, finding, rulePropertyName));
        }
        return findingsJSON;
    }

    private static @NotNull JsonObject createFindingItemJson(@NotNull Gson gson,
                                                             @NotNull Project project,
                                                             @NotNull ScannerFinding finding,
                                                             @NotNull String rulePropertyName) {
        var jsonItem = new JsonObject();
        jsonItem.add("finding", finding.getOriginalJsonData());
        jsonItem.add("appMapUri", createAppMapUriJson(finding));
        jsonItem.add("problemLocation", createProblemLocationJson(finding));
        jsonItem.add("stackLocations", ReadAction.compute(() -> createStackLocationsJson(gson, project, finding)));
        jsonItem.add("ruleInfo", createRuleInfoJson(gson, finding));
        jsonItem.add(rulePropertyName, createRuleInfoJson(gson, finding));
        jsonItem.addProperty("appMapName", findAppMapName(finding));
        return jsonItem;
    }

    /**
     * follows VSCode's "filterFinding"
     */
    private static @Nullable String findAppMapName(@NotNull ScannerFinding finding) {
        var appMapFile = AppMapFiles.findAppMapFileByMetadataFile(finding.getFindingsFile());
        if (appMapFile == null) {
            return null;
        }

        var filename = appMapFile.getName();
        var index = filename.indexOf('.');
        return index == -1 ? filename : filename.substring(0, index);
    }

    private static @NotNull JsonElement createProblemLocationJson(@NotNull ScannerFinding finding) {
        var location = finding.getProblemLocationFromStack();
        if (location == null) {
            return JsonNull.INSTANCE;
        }

        var data = new JsonObject();
        data.add("range", newRangeJson(location));
        data.add("uri", singlePropertyObject("path", location.filePath));
        return data;
    }

    private static @NotNull JsonElement newRangeJson(FileLocation location) {
        return location == null || location.line == null ? JsonNull.INSTANCE : singlePropertyObject("line", location.line);
    }

    private static @NotNull JsonElement createStackLocationsJson(@NotNull Gson gson,
                                                                 @NotNull Project project,
                                                                 @NotNull ScannerFinding finding) {
        var baseFile = finding.getFindingsFile();
        var stackLocations = finding.stack.stream()
                .map(frame -> resolveStackFrame(project, frame, baseFile))
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableList());

        var json = new JsonArray();
        for (var location : stackLocations) {
            json.add(gson.toJsonTree(location));
        }
        return json;
    }

    private static @NotNull JsonElement createRuleInfoJson(@NotNull Gson gson, @NotNull ScannerFinding finding) {
        return gson.toJsonTree(finding.ruleInfo);
    }

    /**
     * follows VSCode's "resolveAppMapUri"
     */
    private static @NotNull JsonElement createAppMapUriJson(@NotNull ScannerFinding finding) {
        var appMapFile = AppMapFiles.findAppMapFileByMetadataFile(finding.getFindingsFile());
        if (appMapFile == null) {
            return JsonNull.INSTANCE;
        }

        // Adds the state of the webview as URI anchor.
        // The AppMap webview needs property "fragment" and doesn't work with a path containing #fragment.
        var state = AppMapFileEditorState.createViewFlowState(finding.getEventId(), finding.relatedEvents).jsonState;
        var json = new JsonObject();
        json.addProperty("path", appMapFile.toNioPath().toString());
        json.addProperty("fragment", state);
        return json;
    }

    private static @Nullable ResolvedStackLocation resolveStackFrame(@NotNull Project project,
                                                                     @NotNull String frame,
                                                                     @NotNull VirtualFile baseFile) {
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
        return new ResolvedStackLocation(nativePath, truncatedPath, location.getZeroBasedLine(0));
    }

    static @NotNull String truncatePath(@NotNull String path, @NotNull Character separator) {
        if (path.length() <= STACK_TRACE_CHARACTER_LIMIT) {
            return path;
        }

        var separatorString = String.valueOf(separator);
        while (path.contains(separatorString) && path.length() > STACK_TRACE_CHARACTER_LIMIT) {
            path = path.substring(path.indexOf(separator) + 1);
        }
        return "..." + separator + path;
    }
}
