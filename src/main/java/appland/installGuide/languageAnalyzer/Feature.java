package appland.installGuide.languageAnalyzer;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Feature {
    @SerializedName("title")
    @Nullable String title;

    @SerializedName("score")
    @NotNull Score score;

    @SerializedName("text")
    @NotNull String text;
}
