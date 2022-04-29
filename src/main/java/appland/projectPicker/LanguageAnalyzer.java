package appland.projectPicker;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public interface LanguageAnalyzer {
    @NotNull Result analyze(@NotNull VirtualFile directory);
}
