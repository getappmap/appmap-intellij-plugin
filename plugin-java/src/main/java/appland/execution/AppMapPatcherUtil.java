package appland.execution;

import appland.AppMapBundle;
import appland.index.AppMapSearchScopes;
import appland.utils.RunConfigurationUtil;
import com.intellij.execution.CantRunException;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.SimpleJavaParameters;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@SuppressWarnings({"UnstableApiUsage", "DialogTitleCapitalization"})
final class AppMapPatcherUtil {
    private AppMapPatcherUtil() {
    }

    /**
     * Computes the needed JVM parameters.
     * If there's no appmap.yml file yet, it's created.
     * This operation is executed as modal task
     *
     * @param project        Current project
     * @param configuration  The run configuration
     * @param javaParameters Parameters to provide context
     * @return The JVM parameters to add to the command line
     * @throws Exception Throw if unable to calculate the parameters
     */
    public static @NotNull List<String> prepareJavaParameters(@NotNull Project project,
                                                              @NotNull RunProfile configuration,
                                                              @NotNull SimpleJavaParameters javaParameters) throws Exception {
        // < 2025.1 either execute this on the EDT or in a background thread without read access
        // 2025.1 seems to execute this with read access if in a background thread, at least in test mode.

        var task = new Task.WithResult<List<String>, Exception>(project, AppMapBundle.get("appMapConfig.creatingConfig"), true) {
            @Override
            protected List<String> compute(@NotNull ProgressIndicator indicator) throws Exception {
                return prepareJavaParametersUnderProgress(project, configuration, javaParameters);
            }
        };
        task.queue();
        return task.getResult();
    }

    @NotNull
    private static List<String> prepareJavaParametersUnderProgress(@NotNull Project project,
                                                                   @NotNull RunProfile configuration,
                                                                   @NotNull SimpleJavaParameters javaParameters) throws CantRunException, IOException {
        var workingDir = ProgramParameterUtils.findWorkingDir(project, javaParameters, configuration);
        if (workingDir == null) {
            throw new CantRunException("Unable to locate working directory to store AppMap files");
        }

        var config = AppMapJavaPackageConfig.findAndUpdateAppMapConfig(workingDir, AppMapSearchScopes.appMapConfigSearchScope(project));
        if (config == null) {
            var module = RunConfigurationUtil.getRunConfigurationModule(project, configuration, workingDir);
            var bestAppMapDirectory = ReadAction.compute(() -> {
                return AppMapJavaConfigUtil.findBestAppMapContentRootDirectory(module, workingDir);
            });
            config = AppMapJavaPackageConfig.createAppMapConfig(module, bestAppMapDirectory, Path.of("tmp", "appmap"));
        }

        return AppMapJvmCommandLinePatcher.createJvmParams(config);
    }
}
