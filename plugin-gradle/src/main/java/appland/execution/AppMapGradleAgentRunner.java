package appland.execution;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration;

/**
 * Runner to execute Gradle run configurations with the AppMap execute {@link AppMapJvmExecutor}.
 */
public class AppMapGradleAgentRunner implements ProgramRunner<RunnerSettings> {
    @Override
    public @NotNull @NonNls String getRunnerId() {
        return "appmap.runner.gradle";
    }

    @Override
    public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
        return AppMapJvmExecutor.EXECUTOR_ID.equals(executorId) && profile instanceof GradleRunConfiguration;
    }

    @Override
    public void execute(@NotNull ExecutionEnvironment environment) throws ExecutionException {
        findDelegate().execute(environment);
    }

    private @NotNull ProgramRunner<RunnerSettings> findDelegate() {
        return (ProgramRunner<RunnerSettings>) ProgramRunner.findRunnerById(ExternalSystemConstants.RUNNER_ID);
    }
}
