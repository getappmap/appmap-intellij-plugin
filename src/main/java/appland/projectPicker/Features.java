package appland.projectPicker;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@AllArgsConstructor
public class Features {
    @SerializedName("lang")
    @NotNull FeatureEx lang;

    @SerializedName("web")
    @Nullable Feature web;

    @SerializedName("test")
    @Nullable Feature test;

    int getTotalScore() {
        return lang.score.value
                + (web == null ? 0 : web.score.value)
                + (test == null ? 0 : test.score.value);
    }
}