package appland.problemsView.model;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public enum ImpactDomain {
    @SerializedName("Security")
    Security("Security"),
    @SerializedName("Performance")
    Performance("Performance"),
    @SerializedName("Stability")
    Stability("Stability"),
    @SerializedName("Maintainability")
    Maintainability("Maintainability");

    private final @NotNull String jsonId;

    ImpactDomain(@NotNull String jsonId) {
        this.jsonId = jsonId;
    }
}
