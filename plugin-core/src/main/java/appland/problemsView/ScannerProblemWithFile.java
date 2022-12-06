package appland.problemsView;

import com.intellij.openapi.vfs.VirtualFile;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Value
public class ScannerProblemWithFile {
    @NotNull ScannerProblem problem;
    @Nullable VirtualFile sourceFile;
}
