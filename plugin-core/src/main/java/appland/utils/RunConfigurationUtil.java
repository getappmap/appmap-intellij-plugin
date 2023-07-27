package appland.utils;

import com.intellij.execution.configurations.ModuleBasedConfiguration;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
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

    public static @NotNull Module getRunConfigurationModule(@NotNull Project project,
                                                            @NotNull RunProfile runProfile,
                                                            @NotNull VirtualFile context) {
        var module = getRunConfigurationModule(runProfile);
        if (module == null) {
            module = ProjectRootManager.getInstance(project).getFileIndex().getModuleForFile(context, false);
        }
        if (module == null) {
            throw new IllegalStateException("unable to locate module to store AppMap files");
        }
        return module;
    }
}
