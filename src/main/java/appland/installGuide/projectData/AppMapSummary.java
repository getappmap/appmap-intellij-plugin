package appland.installGuide.projectData;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;
import org.jetbrains.annotations.Nullable;

@Data
@Builder
public class AppMapSummary {
    @SerializedName("path")
    String path;

    @SerializedName("name")
    @Nullable String name;

    @SerializedName("requests")
    @Nullable Integer requests;

    @SerializedName("sqlQueries")
    @Nullable Integer sqlQueries;

    @SerializedName("functions")
    @Nullable Integer functions;
}
