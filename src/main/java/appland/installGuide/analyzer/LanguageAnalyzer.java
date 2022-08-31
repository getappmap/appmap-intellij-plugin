package appland.installGuide.analyzer;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Extension point to find supported languages, test frameworks, and web frameworks in a directory hierarchy.
 * <p>
 * Implementations follow the VSCode's plugin implementations at
 * <a href="https://github.com/applandinc/vscode-appland/tree/master/src/analyzers">src/analyzers</a>.
 */
public interface LanguageAnalyzer {
    ExtensionPointName<LanguageAnalyzer> EP = ExtensionPointName.create("appland.languageAnalyzer");

    static @Nullable LanguageAnalyzer create(@NotNull Languages.Language language) {
        return EP.findFirstSafe(analyzer -> analyzer.isAccepting(language));
    }

    boolean isAccepting(@NotNull Languages.Language language);

    @NotNull ProjectAnalysis analyze(@NotNull VirtualFile directory);
}
