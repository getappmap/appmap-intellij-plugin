package appland.projectPicker;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class LanguageAnalyzers {
    public static final LanguageAnalyzer JAVA_LANGUAGE_ANALYZER = new JavaLanguageAnalyzer();

    private LanguageAnalyzers() {
    }

    public static @Nullable LanguageAnalyzer create(@NotNull Languages.Language language) {
        switch (language.id) {
            case "java":
                return JAVA_LANGUAGE_ANALYZER;
            default:
                return null;
        }
    }
}
