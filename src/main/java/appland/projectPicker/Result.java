package appland.projectPicker;

import com.google.gson.annotations.SerializedName;
import lombok.Value;
import org.jetbrains.annotations.NotNull;

@Value
public class Result {
    @SerializedName("score")
    int score;
    @SerializedName("name")
    String name;
    @SerializedName("path")
    String path;
    @SerializedName("features")
    Features features;

    public Result(@NotNull Features features, @NotNull String name, @NotNull String filePath) {
        this.features = features;
        this.name = name;
        this.path = filePath;

        this.score = features.getTotalScore();
    }
}
