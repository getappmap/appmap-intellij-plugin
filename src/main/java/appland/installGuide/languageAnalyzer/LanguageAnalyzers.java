package appland.installGuide.languageAnalyzer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class LanguageAnalyzers {
    public static final LanguageAnalyzer JAVA_LANGUAGE_ANALYZER = new JavaLanguageAnalyzer();
    public static final LanguageAnalyzer JAVASCRIPT_LANGUAGE_ANALYZER = new JavaScriptLanguageAnalyzer();

    private LanguageAnalyzers() {
    }

    public static @Nullable LanguageAnalyzer create(@NotNull Languages.Language language) {
        switch (language.id) {
            case "java":
                return JAVA_LANGUAGE_ANALYZER;
            case "javascript":
                return JAVASCRIPT_LANGUAGE_ANALYZER;
            default:
                return null;
        }
    }
}
