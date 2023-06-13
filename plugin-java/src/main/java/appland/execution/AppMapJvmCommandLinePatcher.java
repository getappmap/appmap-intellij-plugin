package appland.execution;

import appland.AppMapBundle;
import appland.javaAgent.AppMapJavaAgentDownloadService;
import com.intellij.execution.CantRunException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

/**
 * Patching of a JVM commandline to add the AppMap agent to the execution of a Java program.
 */
public class AppMapJvmCommandLinePatcher {
    @NotNull
    static List<String> createJvmParams(@Nullable Path appMapConfig, @Nullable Path appMapOutputDirectory) throws CantRunException {
        if (appMapConfig == null) {
            throw new CantRunException("Unable to find an appmap.yml file");
        }

        var agentJarPath = AppMapJavaAgentDownloadService.getInstance().getJavaAgentPathIfExists();
        if (agentJarPath == null) {
            throw new CantRunException(AppMapBundle.get("javaAgent.run.missingJar"));
        }

        var jvmParams = new LinkedList<String>();
        //jvmParams.add("-Dappmap.debug");
        jvmParams.add("-Dappmap.config.file=" + appMapConfig);
        if (appMapOutputDirectory != null) {
            jvmParams.add("-Dappmap.output.directory=" + appMapOutputDirectory);
        }
        jvmParams.add("-javaagent:" + agentJarPath);
        return jvmParams;
    }
}
