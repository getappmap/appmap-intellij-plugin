package appland.installGuide.languageAnalyzer;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum Score {
    Good(2), Okay(1), Bad(0);

    final int value;
}
