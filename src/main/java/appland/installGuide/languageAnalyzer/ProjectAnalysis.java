package appland.installGuide.languageAnalyzer;

import com.google.gson.annotations.SerializedName;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Value
public class ProjectAnalysis {
    @SerializedName("score")
    int score;

    @SerializedName("name")
    String name;

    @SerializedName("path")
    String path;

    @SerializedName("features")
    Features features;

    @SerializedName("nodeVersion")
    @Nullable NodeVersion nodeVersion;

    public ProjectAnalysis(@NotNull String name,
                           @NotNull String filePath,
                           @NotNull Features features,
                           @Nullable NodeVersion nodeVersion) {
        this.name = name;
        this.path = filePath;
        this.features = features;

        this.score = features.getTotalScore();
        this.nodeVersion = nodeVersion;
    }
}
