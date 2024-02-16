package appland.execution;

import appland.AppMapBundle;
import appland.Icons;
import appland.jetbrains.JavaVersion;
import appland.telemetry.TelemetryService;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.ExecutorRegistry;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.NlsActions;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.ToolWindowId;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * Executor to execute JVM-based run configurations with the AppMap agent attached.
 */
public class AppMapJvmExecutor extends Executor {
    public static final String EXECUTOR_ID = "AppMap";
    // latest version of Java, which is supported by the AppMap JVM agent
    private static final int LATEST_SUPPORTED_JAVA_VERSION = 21;

    public static @NotNull Executor getInstance() {
        var executor = ExecutorRegistry.getInstance().getExecutorById(EXECUTOR_ID);
        assert executor != null;
        return executor;
    }

    /**
     * Verify that the JDK is actually supported by the AppMap agent.
     *
     * @param jdk The JDK to verify
     * @throws ExecutionException If the JDK is unsupported
     */
    public static void verifyJDK(@NotNull Project project, @NotNull Sdk jdk) throws ExecutionException {
        var version = JavaVersion.tryParse(jdk.getVersionString());
        if (version != null && version.isAtLeast(LATEST_SUPPORTED_JAVA_VERSION + 1)) {
            TelemetryService.getInstance().sendEvent("run_config:incompatible_jdk",
                    event -> event.property("appmap.jdkVersion", jdk.getVersionString()));

            throw new UnsupportedJdkException(project);
        }
    }

    @Override
    public @NotNull String getToolWindowId() {
        return ToolWindowId.RUN;
    }

    @Override
    public @NotNull Icon getToolWindowIcon() {
        return Icons.TOOL_WINDOW;
    }

    @Override
    public @NotNull Icon getIcon() {
        return Icons.EXECUTE;
    }

    @Override
    public Icon getDisabledIcon() {
        return IconLoader.getDisabledIcon(getIcon());
    }

    @Override
    public @NlsActions.ActionDescription String getDescription() {
        return AppMapBundle.get("appMapExecutor.description");
    }

    @Override
    public @NotNull @NlsActions.ActionText String getActionName() {
        return "AppMap";
    }

    @Override
    public @NotNull @NonNls String getId() {
        return EXECUTOR_ID;
    }

    @Override
    public @NotNull @Nls(capitalization = Nls.Capitalization.Title) String getStartActionText() {
        return getStartActionText("");
    }

    @Override
    public @NotNull String getStartActionText(@NotNull String configurationName) {
        if (StringUtil.isEmpty(configurationName)) {
            return AppMapBundle.get("appMapExecutor.startAction");
        }
        return AppMapBundle.get("appMapExecutor.startActionConfigName", " '" + shortenNameIfNeeded(configurationName) + "'");
    }

    @Override
    public @NonNls String getContextActionId() {
        return "RunAppMap";
    }

    @Override
    public @NonNls String getHelpId() {
        return null;
    }
}
