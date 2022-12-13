package appland.editor;

import appland.problemsView.model.ScannerFindingEvent;
import appland.utils.GsonUtils;
import com.google.gson.JsonObject;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.fileEditor.FileEditorStateLevel;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

@EqualsAndHashCode
@Value
public class AppMapFileEditorState implements FileEditorState {
    public @NotNull String jsonState;

    public AppMapFileEditorState(@NotNull String state) {
        jsonState = state;
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

    @Override
    public boolean canBeMergedWith(@NotNull FileEditorState otherState, @NotNull FileEditorStateLevel level) {
        return otherState instanceof AppMapFileEditorState && otherState.equals(this);
    }
}
