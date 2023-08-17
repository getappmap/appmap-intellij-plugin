package appland.problemsView.model;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public enum TestStatus {
    @SerializedName("failed")
    Failed("failed"),
    @SerializedName("succeeded")
    Succeeded("succeeded");

    private final @NotNull String jsonId;

    TestStatus(@NotNull String jsonId) {
        this.jsonId = jsonId;
    }

    public static @NotNull TestStatus byJsonId(@NotNull String jsonId) {
        if (jsonId.equals(Failed.jsonId)) {
            return Failed;
        }
        if (jsonId.equals(Succeeded.jsonId)) {
            return Succeeded;
        }

        throw new IllegalStateException("Unable to find test status for " + jsonId);
    }
}
