package appland.problemsView.model;

import com.google.gson.annotations.SerializedName;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@EqualsAndHashCode
public class CheckInfo {
    @SerializedName("id")
    public @NotNull String id = "";

    @SerializedName("rule")
    @Nullable RuleInfo ruleInfo;
}
