package appland.execution;

import appland.AppMapBundle;
import appland.notifications.AppMapNotifications;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.configurations.SimpleJavaParameters;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration;
import com.intellij.openapi.externalSystem.service.execution.configuration.ExternalSystemRunConfigurationExtension;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration;

public class AppMapExternalSystemExtension extends ExternalSystemRunConfigurationExtension {
    private static final Logger LOG = Logger.getInstance(AppMapExternalSystemExtension.class);

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
            var gradleRunConfig = (GradleRunConfiguration) configuration;
            var project = gradleRunConfig.getProject();

            try {
                var workingDir = ProgramParameterUtils.findWorkingDir(project, javaParameters);
                if (workingDir == null) {
                    throw new IllegalStateException("unable to locate working directory to store AppMap files");
                }

                var appMapOutputDirectory = workingDir.toNioPath().resolve("build").resolve("appmap");
                var config = AppMapJavaPackageConfig.createOrUpdateAppMapConfig(project,
                        gradleRunConfig,
                        workingDir,
                        appMapOutputDirectory);

                var jvmParams = AppMapJvmCommandLinePatcher.createJvmParams(config, appMapOutputDirectory);
                javaParameters.getVMParametersList().addAll(jvmParams);
            } catch (Exception e) {
                LOG.warn("Unable to execute run configuration", e);
                AppMapNotifications.showExpiringRecordingNotification(project,
                        null,
                        AppMapBundle.get("appMapExecutor.executionError.message", e.getMessage()),
                        NotificationType.ERROR,
                        true);
            }
        }
    }
}