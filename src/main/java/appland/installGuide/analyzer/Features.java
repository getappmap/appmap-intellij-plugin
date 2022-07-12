package appland.installGuide.analyzer;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Data
@AllArgsConstructor
public class Features {
    @SerializedName("language")
    @NotNull FeatureEx lang;

    @SerializedName("webFramework")
    @Nullable Feature web;

    @SerializedName("testFramework")
    @Nullable Feature test;

    int getTotalScore() {
        return lang.score.getScoreValue()
                + (web == null ? 0 : web.score.getScoreValue())
                + (test == null ? 0 : test.score.getScoreValue());
    }
}