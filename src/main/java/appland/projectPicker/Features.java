package appland.projectPicker;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@AllArgsConstructor
public class Features {
    @SerializedName("language")
    @NotNull FeatureEx lang;

    @SerializedName("webFramework")
    @Nullable Feature web;

    @SerializedName("testFramework")
    @Nullable Feature test;

    int getTotalScore() {
        return lang.score.value
                + (web == null ? 0 : web.score.value)
                + (test == null ? 0 : test.score.value);
    }
}