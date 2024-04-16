package appland.execution;

import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.util.text.Strings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.execution.MavenRunConfiguration;

import java.util.List;

public class AppMapMavenProgramPatcher extends AbstractAppMapJavaProgramPatcher {
    @Override
    protected boolean isSupported(@NotNull RunProfile configuration) {
        return configuration instanceof MavenRunConfiguration;
    }

    @Override
    protected void applyJvmParameters(JavaParameters javaParameters, List<String> jvmParams) {
        super.applyJvmParameters(javaParameters, jvmParams);

        // delegate to Maven child processes via argLine. It follows the implementation of the Maven surefire plugin
        // https://github.com/apache/maven-surefire/blob/78805045bb90d7cc5692b6a388e3605d648146d2/surefire-its/src/test/java/org/apache/maven/surefire/its/fixture/SurefireLauncher.java#L372
        var existingArgLine = StringUtil.defaultIfEmpty(javaParameters.getVMParametersList().getPropertyValue("argLine"), "");
        var appMapArgLine = Strings.join(jvmParams, " ");
        if (!existingArgLine.isEmpty()) {
            appMapArgLine = existingArgLine + " " + appMapArgLine;
        }

        javaParameters.getVMParametersList().add("-DargLine=" + appMapArgLine);
    }
}
