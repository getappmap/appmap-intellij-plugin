package appland.execution;

import com.intellij.execution.Executor;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.runners.JavaProgramPatcher;
import com.intellij.openapi.diagnostic.Logger;

/**
 * Patches IntelliJ Java run configurations with the AppMap agent.
 */
public class AppMapJavaProgramPatcher extends JavaProgramPatcher {
    @Override
    public void patchJavaParameters(Executor executor, RunProfile configuration, JavaParameters javaParameters) {
        if (executor instanceof AppMapJvmExecutor && configuration instanceof RunConfiguration) {
            try {
                var project = ((RunConfiguration) configuration).getProject();
                var workingDir = ProgramParameterUtils.findWorkingDir(project, javaParameters);
                var config = AppMapJavaPackageConfig.findOrCreateAppMapConfig(project, configuration, workingDir);

                AppMapJvmCommandLinePatcher.patchSimpleJavaParameters(javaParameters, config);
            } catch (Exception e) {
                Logger.getInstance(AppMapJavaProgramPatcher.class).error(e);
            }
        }
    }
}
