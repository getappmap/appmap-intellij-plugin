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
    public @NotNull FeatureEx lang;

    @SerializedName("webFramework")
    public @Nullable Feature web;

    @SerializedName("testFramework")
    public @Nullable Feature test;

    int getTotalScore() {
        var languageScore = lang.score;
        var webScore = web == null ? Score.Bad : web.score;
        var testScore = test == null ? Score.Bad : test.score;

        // score edge cases
        if (languageScore == Score.Bad) {
            return 1;
        }
        if (languageScore == Score.Good && webScore == Score.Good && testScore == Score.Good) {
            return 2;
        }

        return languageScore.getScoreValue() + webScore.getScoreValue() + testScore.getScoreValue();
    }
}