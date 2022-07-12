package appland.installGuide.analyzer;

import com.google.gson.annotations.SerializedName;
import lombok.Value;
import org.jetbrains.annotations.NotNull;

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

    public ProjectAnalysis(@NotNull String name,
                           @NotNull String filePath,
                           @NotNull Features features) {
        this.name = name;
        this.path = filePath;
        this.features = features;

        this.score = features.getTotalScore();
    }
}
