package appland.execution;

import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.openapi.util.text.Strings;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.execution.MavenRunConfiguration;

import java.nio.file.Path;
import java.util.List;

public class AppMapMavenProgramPatcher extends AbstractAppMapJavaProgramPatcher {
    @Override
    protected boolean isSupported(@NotNull RunProfile configuration) {
        return configuration instanceof MavenRunConfiguration;
    }

    @Override
    protected @Nullable Path findAppMapOutputDirectory(@NotNull RunProfile configuration,
                                                       @NotNull VirtualFile workingDirectory) {
        return workingDirectory.toNioPath().resolve("target").resolve("appmap");
    }

    @Override
    protected void applyJvmParameters(JavaParameters javaParameters, List<String> jvmParams) {
        super.applyJvmParameters(javaParameters, jvmParams);

        // delegate to Maven child processes via argLine. It follows the implementation of the Maven surefire plugin
        // https://github.com/apache/maven-surefire/blob/78805045bb90d7cc5692b6a388e3605d648146d2/surefire-its/src/test/java/org/apache/maven/surefire/its/fixture/SurefireLauncher.java#L372
        javaParameters.getVMParametersList().add("-DargLine=" + Strings.join(jvmParams, " "));
    }
}
