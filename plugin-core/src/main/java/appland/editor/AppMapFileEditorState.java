package appland.editor;

import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.fileEditor.FileEditorStateLevel;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jetbrains.annotations.NotNull;

@EqualsAndHashCode
@Value
public class AppMapFileEditorState implements FileEditorState {
    public @NotNull String jsonState;

    public AppMapFileEditorState(@NotNull String state) {
        jsonState = state;
    }

    @Override
    public boolean canBeMergedWith(@NotNull FileEditorState otherState, @NotNull FileEditorStateLevel level) {
        return otherState instanceof AppMapFileEditorState && otherState.equals(this);
    }
}
