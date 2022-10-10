package appland.execution;

import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.impl.DefaultJavaProgramRunner;
import com.intellij.execution.runners.JavaProgramPatcher;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractAppMapJavaAgentRunner extends DefaultJavaProgramRunner {
    @Override
    public abstract @NotNull @NonNls String getRunnerId();

    @Override
    public abstract boolean canRun(@NotNull String executorId, @NotNull RunProfile profile);

    @Override
    public void patch(@NotNull JavaParameters javaParameters,
                      @Nullable RunnerSettings settings,
                      @NotNull RunProfile runProfile,
                      boolean beforeExecution) {
        try {
            // invokes our AppMapJavaProgramPatcher, but inside a read action.
            JavaProgramPatcher.runCustomPatchers(javaParameters, AppMapJvmExecutor.getInstance(), runProfile);
        } catch (Exception e) {
            throw new RuntimeException("Failed to update JVM command line for AppMap", e);
        }
    }
}
