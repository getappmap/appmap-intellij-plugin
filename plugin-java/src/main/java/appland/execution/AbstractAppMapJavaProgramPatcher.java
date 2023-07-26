package appland.execution;

import appland.AppMapBundle;
import appland.notifications.AppMapNotifications;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.runners.JavaProgramPatcher;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;

public abstract class AbstractAppMapJavaProgramPatcher extends JavaProgramPatcher {
    private static final Logger LOG = Logger.getInstance(AbstractAppMapJavaProgramPatcher.class);

    @Override
    public void patchJavaParameters(Executor executor, RunProfile configuration, JavaParameters javaParameters) {
        if (!(executor instanceof AppMapJvmExecutor) || !isSupported(configuration)) {
            return;
        }

        var project = ((RunConfiguration) configuration).getProject();
        try {
            var workingDir = ProgramParameterUtils.findWorkingDir(project, javaParameters);
            if (workingDir == null) {
                throw new IllegalStateException("unable to locate working directory to store AppMap files");
            }

            var workingDirPath = workingDir.toNioPath();
            var appMapOutputDirectory = findAppMapOutputDirectory(configuration, workingDir);
            if (appMapOutputDirectory == null) {
                // default to tmp/appmap
                appMapOutputDirectory = workingDirPath.resolve("tmp").resolve("appmap");
            }

            var config = AppMapJavaPackageConfig.createOrUpdateAppMapConfig(project,
                    configuration,
                    workingDir,
                    appMapOutputDirectory);

            var jvmParams = AppMapJvmCommandLinePatcher.createJvmParams(config, appMapOutputDirectory);
            applyJvmParameters(javaParameters, jvmParams);
        } catch (Exception e) {
            LOG.warn("Unable to execute run configuration", e);
            AppMapNotifications.showExpiringRecordingNotification(project,
                    null,
                    AppMapBundle.get("appMapExecutor.executionError.message", e.getMessage()),
                    NotificationType.ERROR,
                    true);
        }
    }

    protected abstract boolean isSupported(@NotNull RunProfile configuration);

    protected abstract @Nullable Path findAppMapOutputDirectory(@NotNull RunProfile configuration,
                                                                @NotNull VirtualFile workingDirectory);

    protected void applyJvmParameters(JavaParameters javaParameters, List<String> jvmParams) {
        javaParameters.getVMParametersList().addAll(jvmParams);
    }
}
