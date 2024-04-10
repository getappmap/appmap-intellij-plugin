package appland.execution;

import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.SimpleProgramParameters;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Paths;

public final class ProgramParameterUtils {
    public static @Nullable VirtualFile findWorkingDir(@NotNull Project project,
                                                       @NotNull SimpleProgramParameters programParameters,
                                                       @NotNull RunProfile configuration) {
        var workingDir = programParameters.getWorkingDirectory();
        if (workingDir != null) {
            // seems to be a native path
            return LocalFileSystem.getInstance().findFileByNioFile(Paths.get(workingDir));
        }

        // External run configurations define the working directory based on the selected project
        if (configuration instanceof ExternalSystemRunConfiguration) {
            var externalPath = ((ExternalSystemRunConfiguration) configuration).getSettings().getExternalProjectPath();
            if (externalPath != null) {
                // Seems to be used as native path by the SDK,
                // e.g. by org.jetbrains.plugins.gradle.execution.build.TasksExecutionSettingsBuilder#build.
                return LocalFileSystem.getInstance().findFileByNioFile(Paths.get(externalPath));
            }
        }

        return ProjectUtil.guessProjectDir(project);
    }
}
