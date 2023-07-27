package appland.execution;

import com.intellij.execution.JavaRunConfigurationBase;
import com.intellij.execution.configurations.RunProfile;
import org.jetbrains.annotations.NotNull;

/**
 * Patches IntelliJ Java run configurations with the AppMap agent.
 */
public class AppMapJavaProgramPatcher extends AbstractAppMapJavaProgramPatcher {
    @Override
    protected boolean isSupported(@NotNull RunProfile configuration) {
        return configuration instanceof JavaRunConfigurationBase;
    }
}
