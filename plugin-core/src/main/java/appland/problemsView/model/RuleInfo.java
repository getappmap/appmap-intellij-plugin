package appland.problemsView.model;

import com.google.gson.annotations.SerializedName;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

// follows VSCode's "RuleInfo" type
@EqualsAndHashCode
public class RuleInfo {
    @SerializedName("id")
    public @NotNull String id = "";

    @SerializedName("description")
    public @NotNull String description = "";

    @SerializedName("title")
    public @NotNull String title = "";

    @SerializedName("references")
    public @Nullable Map<String, String> references = null;

    @SerializedName("impactDomain")
    public @Nullable ImpactDomain impactDomain = null;

    @SerializedName("labels")
    public @Nullable List<String> labels = null;

    @SerializedName("scope")
    public @Nullable String scope = null;
}
