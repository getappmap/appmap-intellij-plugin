package appland.execution;

import com.intellij.execution.configurations.SimpleProgramParameters;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Paths;

public final class ProgramParameterUtils {
    public static @Nullable VirtualFile findWorkingDir(@NotNull Project project, @NotNull SimpleProgramParameters programParameters) {
        // seems to be a native path
        var workingDir = programParameters.getWorkingDirectory();
        return workingDir == null
                ? ProjectUtil.guessProjectDir(project)
                : LocalFileSystem.getInstance().findFileByNioFile(Paths.get(workingDir));
    }
}
