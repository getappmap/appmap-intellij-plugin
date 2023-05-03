package appland.webviews.appMap;

import appland.problemsView.model.ScannerFindingEvent;
import appland.utils.GsonUtils;
import com.google.gson.JsonObject;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

@EqualsAndHashCode
@Value
public class AppMapFileEditorState {
    public @NotNull String jsonState;

    private AppMapFileEditorState(@NotNull String state) {
        jsonState = state;
    }

    public static final AppMapFileEditorState EMPTY = new AppMapFileEditorState("{}");

    public static AppMapFileEditorState of(@NotNull String jsonState) {
        return new AppMapFileEditorState(jsonState);
    }

    public static AppMapFileEditorState createViewFlowState(@Nullable Integer eventId, @Nullable List<ScannerFindingEvent> relatedEvents) {
        var state = new JsonObject();
        state.addProperty("currentView", "viewFlow");
        if (eventId != null) {
            state.addProperty("selectedObject", "event:" + eventId);
        }
        if (relatedEvents != null && !relatedEvents.isEmpty()) {
            var joinedEvents = relatedEvents.stream()
                    .map(event -> "id:" + event.id)
                    .collect(Collectors.joining(" "));
            state.addProperty("traceFilter", joinedEvents);
        }
        return new AppMapFileEditorState(GsonUtils.GSON.toJson(state));
    }

    public static @NotNull AppMapFileEditorState createCodeObjectState(@NotNull String nodeId) {
        var state = new JsonObject();
        state.addProperty("currentView", "viewComponent");
        state.addProperty("selectedObject", nodeId);
        return new AppMapFileEditorState(GsonUtils.GSON.toJson(state));
    }
}
