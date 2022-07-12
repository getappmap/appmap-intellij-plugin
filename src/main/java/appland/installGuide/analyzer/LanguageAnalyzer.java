package appland.installGuide.analyzer;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public interface LanguageAnalyzer {
    @NotNull ProjectAnalysis analyze(@NotNull VirtualFile directory);
}
