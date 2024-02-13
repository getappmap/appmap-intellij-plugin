package appland.execution;

import appland.AppMapBundle;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration;
import org.jetbrains.plugins.gradle.settings.GradleSettings;
import org.jetbrains.plugins.gradle.util.GradleConstants;

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
        return AppMapJvmExecutor.EXECUTOR_ID.equals(executorId)
                && profile instanceof GradleRunConfiguration
                && !DumbService.isDumb(((GradleRunConfiguration) profile).getProject());
    }

    @Override
    public void execute(@NotNull ExecutionEnvironment environment) throws ExecutionException {
        verifyJdk(environment);
        findDelegate().execute(environment);
    }

    private @NotNull ProgramRunner<RunnerSettings> findDelegate() {
        return (ProgramRunner<RunnerSettings>) ProgramRunner.findRunnerById(ExternalSystemConstants.RUNNER_ID);
    }

    private static void verifyJdk(@NotNull ExecutionEnvironment environment) throws ExecutionException {
        var manager = ExternalSystemApiUtil.getManager(GradleConstants.SYSTEM_ID);
        if (manager != null) {
            var project = environment.getProject();
            var gradleProjectPath = ((GradleRunConfiguration) environment.getRunProfile()).getSettings().getExternalProjectPath();
            var gradleProjectSettings = GradleSettings.getInstance(project).getLinkedProjectSettings(gradleProjectPath);
            if (gradleProjectSettings != null) {
                var jdk = getGradleSdkWithProgress(project, gradleProjectSettings.getGradleJvm());
                if (jdk != null) {
                    AppMapJvmExecutor.verifyJDK(project, jdk);
                }
            }
        }
    }

    /**
     * Because @{link {@link ExternalSystemJdkUtil#getJdk(Project, String)}} is a SlowOperation in 2023.2,
     * we need to move this into a background thread.
     * Refer to https://github.com/getappmap/appmap-intellij-plugin/issues/566 for a stacktrace.
     *
     * @return The Gradle JVM SDK, if available.
     */
    private static @Nullable Sdk getGradleSdkWithProgress(@NotNull Project project,
                                                          @Nullable String gradleJvmName) {
        var title = AppMapBundle.get("appMapExecutor.validatingGradleJDK");
        var task = new Task.WithResult<Sdk, Exception>(project, title, true) {
            @Override
            protected Sdk compute(@NotNull ProgressIndicator indicator) {
                return ExternalSystemJdkUtil.getJdk(project, gradleJvmName);
            }
        };
        task.queue();

        try {
            return task.getResult();
        } catch (Exception e) {
            return null;
        }
    }
}
