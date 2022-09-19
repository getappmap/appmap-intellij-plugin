package appland.execution;

import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.impl.DefaultJavaProgramRunner;
import com.intellij.execution.runners.JavaProgramPatcher;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.execution.MavenRunConfiguration;

/**
 * Runner to execute Java run configurations (i.e. run configurations not using a build system)
 * with the AppMap executor {@link AppMapJvmExecutor}.
 */
public class AppMapMavenAgentRunner extends DefaultJavaProgramRunner {
    @Override
    public @NotNull @NonNls String getRunnerId() {
        return "appmap.runner.maven";
    }

    @Override
    public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
        return AppMapJvmExecutor.EXECUTOR_ID.equals(executorId) && profile instanceof MavenRunConfiguration;
    }

    @Override
    public void patch(@NotNull JavaParameters javaParameters,
                      @Nullable RunnerSettings settings,
                      @NotNull RunProfile runProfile,
                      boolean beforeExecution) {
        // invokes our AppMapJavaProgramPatcher
        JavaProgramPatcher.runCustomPatchers(javaParameters, AppMapJvmExecutor.getInstance(), runProfile);
    }
}
