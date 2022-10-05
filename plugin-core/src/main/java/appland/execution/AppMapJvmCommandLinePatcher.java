package appland.execution;

import appland.AppMapPlugin;
import com.intellij.execution.CantRunException;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.SimpleJavaParameters;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

/**
 * Patching of a JVM commandline to add the AppMap agent to the execution of a Java program.
 */
public class AppMapJvmCommandLinePatcher {
    static void patchSimpleJavaParameters(@NotNull SimpleJavaParameters parameters, @Nullable Path appMapConfig) throws ExecutionException {
        if (appMapConfig == null) {
            throw new CantRunException("Unable to find an appmap.yml file");
        }

        var vmParams = parameters.getVMParametersList();
        //vmParams.add("-Dappmap.debug");
        vmParams.add("-Dappmap.config.file=" + appMapConfig);
        vmParams.add("-javaagent:" + AppMapPlugin.getJavaAgentPath());

        //var classPath = parameters.getClassPath();
        //classPath.add(AppMapPlugin.getJavaAgentPath().toString());
    }
}
