package appland.problemsView;

import appland.files.AppMapFiles;
import appland.files.FileLocation;
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
     * @param gson     To read and write JSON
     * @param project  Current project
     * @param findings Findings to transform
     * @return JSON array, will be empty for an empty list of findings
     */
    public static @NotNull JsonArray createFindingsArray(@NotNull Gson gson,
                                                         @NotNull Project project,
                                                         @NotNull List<ScannerProblem> findings) {
        var findingsJSON = new JsonArray();
        for (var finding : findings) {
            findingsJSON.add(createFindingItemJson(gson, project, finding));
        }
        return findingsJSON;
    }

    private static @NotNull JsonObject createFindingItemJson(@NotNull Gson gson,
                                                             @NotNull Project project,
                                                             @NotNull ScannerProblem finding) {
        var jsonItem = new JsonObject();
        jsonItem.add("finding", gson.toJsonTree(finding.getFinding()));
        jsonItem.add("appMapUri", createAppMapUriJson(finding));
        jsonItem.add("problemLocation", createProblemLocationJson(finding));
        jsonItem.add("stackLocations", ReadAction.compute(() -> createStackLocationsJson(gson, project, finding)));
        jsonItem.add("ruleInfo", createRuleInfoJson(gson, finding));
        jsonItem.addProperty("appMapName", findAppMapName(finding));
        return jsonItem;
    }

    /**
     * follows VSCode's "filterFinding"
     */
    private static @Nullable String findAppMapName(@NotNull ScannerProblem finding) {
        var appMapFile = AppMapFiles.findAppMapSourceFile(finding.getFindingsFile());
        if (appMapFile == null) {
            return null;
        }

        var filename = appMapFile.getName();
        var index = filename.indexOf('.');
        return index == -1 ? filename : filename.substring(0, index);
    }

    private static @NotNull JsonElement createProblemLocationJson(@NotNull ScannerProblem finding) {
        var location = finding.getFinding().getProblemLocation();
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
                                                                 @NotNull ScannerProblem finding) {
        var baseFile = finding.getFindingsFile();
        var stackLocations = finding.getFinding().stack.stream().map(frame -> resolveStackFrame(project, frame, baseFile)).filter(Objects::nonNull).collect(Collectors.toUnmodifiableList());

        var json = new JsonArray();
        for (var location : stackLocations) {
            json.add(gson.toJsonTree(location));
        }
        return json;
    }

    private static @NotNull JsonElement createRuleInfoJson(@NotNull Gson gson, @NotNull ScannerProblem finding) {
        return gson.toJsonTree(finding.getFinding().ruleInfo);
    }

    /**
     * follows VSCode's "resolveAppMapUri"
     */
    private static @NotNull JsonElement createAppMapUriJson(@NotNull ScannerProblem finding) {
        var appMapFile = AppMapFiles.findAppMapSourceFile(finding.getFindingsFile());
        if (appMapFile == null) {
            return JsonNull.INSTANCE;
        }

        // adds the state of the webview as URI anchor
        var state = AppMapFileEditorState.createViewFlowState(finding.getFinding().getEventId(), finding.getFinding().relatedEvents).jsonState;
        return singlePropertyObject("path", appMapFile.toNioPath() + "#" + state);
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
        return new ResolvedStackLocation(nativePath, truncatedPath + location.getSuffix(), location.getZeroBasedLine(0));
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
