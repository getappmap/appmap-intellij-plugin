package appland.utils;

import com.intellij.execution.configurations.ModuleBasedConfiguration;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.openapi.module.Module;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class RunConfigurationUtil {
    private RunConfigurationUtil() {
    }

    public static @Nullable Module getRunConfigurationModule(@NotNull RunProfile runProfile) {
        if (!(runProfile instanceof ModuleBasedConfiguration)) {
            return null;
        }

        var module = ((ModuleBasedConfiguration<?, ?>) runProfile).getConfigurationModule();
        return module != null ? module.getModule() : null;
    }
}
