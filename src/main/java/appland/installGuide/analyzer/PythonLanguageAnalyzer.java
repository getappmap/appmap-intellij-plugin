package appland.installGuide.analyzer;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class PythonLanguageAnalyzer implements LanguageAnalyzer {
    @Override
    public boolean isAccepting(Languages.@NotNull Language language) {
        return "python".equals(language.id);
    }

    @Override
    public @NotNull ProjectAnalysis analyze(@NotNull VirtualFile directory) {
        return null;
    }
}
