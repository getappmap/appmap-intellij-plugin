package appland.execution;

import appland.AppMapPlugin;
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
    static List<String> createJvmParams(@Nullable Path appMapOutputDirectory) throws CantRunException {
        var jvmParams = new LinkedList<String>();
        //jvmParams.add("-Dappmap.debug");
        if (appMapOutputDirectory != null) {
            jvmParams.add("-Dappmap.output.directory=" + appMapOutputDirectory);
        }
        jvmParams.add("-javaagent:" + AppMapPlugin.getJavaAgentPath());
        return jvmParams;
    }
}
