package appland.execution;

import com.intellij.execution.Executor;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.configurations.SimpleJavaParameters;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration;
import com.intellij.openapi.externalSystem.service.execution.configuration.ExternalSystemRunConfigurationExtension;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration;

public class AppMapExternalSystemExtension extends ExternalSystemRunConfigurationExtension {
    @Override
    public boolean isApplicableFor(@NotNull ExternalSystemRunConfiguration configuration) {
        return configuration instanceof GradleRunConfiguration;
    }

    @Override
    public void updateVMParameters(@NotNull ExternalSystemRunConfiguration configuration,
                                   @NotNull SimpleJavaParameters javaParameters,
                                   @Nullable RunnerSettings runnerSettings,
                                   @NotNull Executor executor) {
        if (executor instanceof AppMapJvmExecutor) {
            try {
                var gradleRunConfig = (GradleRunConfiguration) configuration;
                var project = gradleRunConfig.getProject();
                var workingDir = ProgramParameterUtils.findWorkingDir(project, javaParameters);
                var config = AppMapJavaPackageConfig.findOrCreateAppMapConfig(project, gradleRunConfig, workingDir);

                AppMapJvmCommandLinePatcher.patchSimpleJavaParameters(javaParameters, config);
            } catch (Exception e) {
                Logger.getInstance(AppMapExternalSystemExtension.class).error(e);
            }
        }
    }
}
