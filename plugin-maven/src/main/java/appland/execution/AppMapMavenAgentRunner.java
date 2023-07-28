package appland.execution;

import com.intellij.execution.configurations.RunProfile;
import com.intellij.openapi.project.DumbService;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.execution.MavenRunConfiguration;

/**
 * Runner to execute Java run configurations (i.e. run configurations not using a build system)
 * with the AppMap executor {@link AppMapJvmExecutor}.
 */
public class AppMapMavenAgentRunner extends AbstractAppMapJavaAgentRunner {
    @Override
    public @NotNull @NonNls String getRunnerId() {
        return "appmap.runner.maven";
    }

    @Override
    public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
        return AppMapJvmExecutor.EXECUTOR_ID.equals(executorId)
                && profile instanceof MavenRunConfiguration
                && !DumbService.isDumb(((MavenRunConfiguration) profile).getProject());
    }
}
