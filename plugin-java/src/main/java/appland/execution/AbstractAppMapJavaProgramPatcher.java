package appland.execution;

import appland.AppMapBundle;
import appland.notifications.AppMapNotifications;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class AbstractAppMapJavaProgramPatcher implements AppMapProgramPatcher {
    private static final Logger LOG = Logger.getInstance(AbstractAppMapJavaProgramPatcher.class);

    protected abstract boolean isSupported(@NotNull RunProfile configuration);

    @Override
    public void patchJavaParameters(@NotNull Executor executor,
                                    @NotNull RunProfile configuration,
                                    @NotNull JavaParameters javaParameters) {
        if (executor instanceof AppMapJvmExecutor && isSupported(configuration)) {
            var project = ((RunConfiguration) configuration).getProject();
            try {
                var jvmParams = AppMapPatcherUtil.prepareJavaParameters(project, configuration, javaParameters);
                applyJvmParameters(javaParameters, jvmParams);
            } catch (Exception e) {
                LOG.warn("Unable to execute run configuration", e);
                AppMapNotifications.showSimpleNotification(project,
                        null,
                        AppMapBundle.get("appMapExecutor.executionError.message", e.getMessage()),
                        NotificationType.ERROR,
                        true);
            }
        }
    }

    protected void applyJvmParameters(JavaParameters javaParameters, List<String> jvmParams) {
        javaParameters.getVMParametersList().addAll(jvmParams);
    }
}
