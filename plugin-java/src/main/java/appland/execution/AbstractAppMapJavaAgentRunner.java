package appland.execution;

import appland.AppMapBundle;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.JavaCommandLineState;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.impl.DefaultJavaProgramRunner;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.JavaProgramPatcher;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbService;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractAppMapJavaAgentRunner extends DefaultJavaProgramRunner {
    @Override
    public abstract @NotNull @NonNls String getRunnerId();

    @Override
    public abstract boolean canRun(@NotNull String executorId, @NotNull RunProfile profile);

    @Override
    public void execute(@NotNull ExecutionEnvironment environment) throws ExecutionException {
        verifyJdk(environment);
        try {
            super.execute(environment);
        } finally {
            AppMapJvmExecutor.sendTelemetry(environment.getRunProfile());
        }
    }

    @Override
    public void patch(@NotNull JavaParameters javaParameters,
                      @Nullable RunnerSettings settings,
                      @NotNull RunProfile runProfile,
                      boolean beforeExecution) {

        try {
            // run our own patchers outside a ReadAction
            AppMapProgramPatcher.EP_NAME.forEachExtensionSafe(patcher -> {
                patcher.patchJavaParameters(AppMapJvmExecutor.getInstance(), runProfile, javaParameters);
            });

            // Invokes all other patchers in a ReadAction.
            // Don't wrap this in "JavaProgramPatcher.patchJavaCommandLineParamsUnderProgress",
            // because com.intellij.execution.impl.DefaultJavaProgramRunner.doExecute
            // is already using it when it's calling this patch method.
            JavaProgramPatcher.runCustomPatchers(javaParameters, AppMapJvmExecutor.getInstance(), runProfile);
        } catch (Exception e) {
            throw new RuntimeException("Failed to update JVM command line for AppMap", e);
        }
    }

    /**
     * Validates the JDK if there's one configured for the executed configuration.
     * Because this method is executed on the EDT
     * and because we need to throw a ExecutionException to cancel the execution of the run configuration,
     * we have to execute a modal task.
     *
     * @param environment Current environment
     * @throws ExecutionException Thrown if the JDK is invalid
     */
    private void verifyJdk(@NotNull ExecutionEnvironment environment) throws ExecutionException {
        var state = environment.getState();
        if (!(state instanceof JavaCommandLineState) || DumbService.isDumb(environment.getProject())) {
            return;
        }

        var task = new Task.WithResult<Void, ExecutionException>(environment.getProject(), AppMapBundle.get("appMapExecutor.verifyingJDK"), false) {
            @Override
            protected Void compute(@NotNull ProgressIndicator indicator) throws ExecutionException {
                var javaParameters = ((JavaCommandLineState) state).getJavaParameters();
                if (javaParameters == null) {
                    return null;
                }

                var jdk = javaParameters.getJdk();
                if (jdk == null) {
                    return null;
                }

                return ReadAction.compute(() -> {
                    AppMapJvmExecutor.verifyJDK(environment.getProject(), jdk);
                    return null;
                });
            }
        };

        task.queue();
        task.getResult();
    }
}
