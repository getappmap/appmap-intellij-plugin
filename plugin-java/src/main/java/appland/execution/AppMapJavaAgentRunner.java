package appland.execution;

import com.intellij.execution.JavaRunConfigurationBase;
import com.intellij.execution.configurations.RunProfile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 * Runner to execute Java run configurations (i.e. run configurations not using a build system)
 * with the AppMap executor {@link AppMapJvmExecutor}.
 */
public class AppMapJavaAgentRunner extends AbstractAppMapJavaAgentRunner {
    @Override
    public @NotNull @NonNls String getRunnerId() {
        return "appmap.runner.java";
    }

    @Override
    public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
        return AppMapJvmExecutor.EXECUTOR_ID.equals(executorId) && profile instanceof JavaRunConfigurationBase;
    }
}
