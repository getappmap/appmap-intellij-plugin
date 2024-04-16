package appland.execution;

import appland.AppMapBundle;
import appland.AppMapPlugin;
import appland.javaAgent.AppMapJavaAgentDownloadService;
import com.intellij.execution.CantRunException;
import com.intellij.openapi.util.registry.Registry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

/**
 * Patching of a JVM commandline to add the AppMap agent to the execution of a Java program.
 */
public final class AppMapJvmCommandLinePatcher {
    private AppMapJvmCommandLinePatcher() {
    }

    @NotNull
    static List<String> createJvmParams(@Nullable Path appMapConfig) throws CantRunException {
        if (appMapConfig == null) {
            throw new CantRunException("Unable to find an appmap.yml file");
        }

        var agentJarPath = AppMapJavaAgentDownloadService.getInstance().getJavaAgentPathIfExists();
        if (agentJarPath == null) {
            agentJarPath = AppMapPlugin.getAppMapJavaAgentPath();
        }
        if (!Files.isReadable(agentJarPath)) {
            throw new CantRunException(AppMapBundle.get("javaAgent.run.unavailableJar"));
        }

        var jvmParams = new LinkedList<String>();
        jvmParams.add("-Dappmap.config.file=" + appMapConfig);
        jvmParams.add("-javaagent:" + agentJarPath);
        if (Registry.is("appmap.agent.debug")) {
            jvmParams.add("-Dappmap.disableLogFile=false");
        }
        return jvmParams;
    }
}
