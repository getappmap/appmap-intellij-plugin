package appland.projectPicker;

import lombok.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Value
class Result {
    Features features;
    int score;
    String name;
    String path;

    public Result(@NotNull Features features, @NotNull String name, @NotNull String filePath) {
        this.features = features;
        this.name = name;
        this.path = filePath;

        this.score = features.getTotalScore();
    }
}

@AllArgsConstructor
class Features {
    @NotNull FeatureEx lang;
    @Nullable Feature web;
    @Nullable Feature test;

    int getTotalScore() {
        return lang.score.value
                + (web == null ? 0 : web.score.value)
                + (test == null ? 0 : test.score.value);
    }
}

@NoArgsConstructor
@AllArgsConstructor
@ToString
class Feature {
    @Nullable String title;
    @NotNull Score score;
    @NotNull String text;
}

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
class FeatureEx extends Feature {
    @Nullable String depFile;
    @Nullable String plugin;
    @Nullable String pluginType;
}

@AllArgsConstructor
enum Score {
    Good(2), Okay(1), Bad(0);

    int value;
}