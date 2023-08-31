package appland.installGuide.analyzer;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Score {
    Bad(0), Okay(1), Good(2);

    private final int scoreValue;
}
