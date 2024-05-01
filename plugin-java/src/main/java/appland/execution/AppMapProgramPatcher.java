package appland.execution;

import com.intellij.execution.Executor;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.openapi.extensions.ExtensionPointName;
import org.jetbrains.annotations.NotNull;

/**
 * Our own extension to patch Java program parameters.
 * We can't use {@link com.intellij.execution.runners.JavaProgramPatcher}, because its patch method
 * is always executed in a ReadAction. Our own patching must happen outside a ReadAction, because we need to run
 * tasks.
 */
public interface AppMapProgramPatcher {
    ExtensionPointName<AppMapProgramPatcher> EP_NAME = ExtensionPointName.create("appland.execution.programPatcher");

    void patchJavaParameters(@NotNull Executor executor, @NotNull RunProfile configuration, @NotNull JavaParameters javaParameters);
}
