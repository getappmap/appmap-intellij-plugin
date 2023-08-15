package appland.problemsView.model;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public enum TestStatus {
    @SerializedName("succeeded")
    Succeeded("succeeded"),
    @SerializedName("failed")
    Failed("failed");

    private final @NotNull String jsonId;

    TestStatus(@NotNull String jsonId) {
        this.jsonId = jsonId;
    }
}
