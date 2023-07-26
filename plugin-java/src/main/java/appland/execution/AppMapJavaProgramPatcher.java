package appland.execution;

import appland.utils.RunConfigurationUtil;
import com.intellij.execution.JavaRunConfigurationBase;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.openapi.compiler.CompilerPaths;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

/**
 * Patches IntelliJ Java run configurations with the AppMap agent.
 */
public class AppMapJavaProgramPatcher extends AbstractAppMapJavaProgramPatcher {
    @Override
    protected boolean isSupported(@NotNull RunProfile configuration) {
        return configuration instanceof JavaRunConfigurationBase;
    }

    @Override
    protected @Nullable Path findAppMapOutputDirectory(@NotNull RunProfile configuration,
                                                       @NotNull VirtualFile workingDirectory) {
        var module = RunConfigurationUtil.getRunConfigurationModule(configuration);
        if (module != null) {
            var classesPath = CompilerPaths.getModuleOutputDirectory(module, false);
            if (classesPath != null) {
                return classesPath.getParent().toNioPath().resolve("appmap");
            }
        }
        return null;
    }
}
