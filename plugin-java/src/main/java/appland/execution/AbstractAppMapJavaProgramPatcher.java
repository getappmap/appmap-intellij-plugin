package appland.execution;

import com.intellij.execution.Executor;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.runners.JavaProgramPatcher;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

public abstract class AbstractAppMapJavaProgramPatcher extends JavaProgramPatcher {
    @Override
    public void patchJavaParameters(Executor executor, RunProfile configuration, JavaParameters javaParameters) {
        if (executor instanceof AppMapJvmExecutor && isSupported(configuration)) {
            try {
                var project = ((RunConfiguration) configuration).getProject();
                var workingDir = ProgramParameterUtils.findWorkingDir(project, javaParameters);
                var config = AppMapJavaPackageConfig.findOrCreateAppMapConfig(project, configuration, workingDir);
                var outputDirectory = findAppMapOutputDirectory(configuration, workingDir);

                AppMapJvmCommandLinePatcher.patchSimpleJavaParameters(javaParameters, config, outputDirectory);
            } catch (Exception e) {
                Logger.getInstance(AppMapJavaProgramPatcher.class).error(e);
            }
        }
    }

    protected abstract boolean isSupported(@NotNull RunProfile configuration);

    protected abstract @Nullable Path findAppMapOutputDirectory(@NotNull RunProfile configuration,
                                                                @Nullable VirtualFile workingDirectory);
}
