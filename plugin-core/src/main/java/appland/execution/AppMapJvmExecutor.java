package appland.execution;

import appland.AppMapBundle;
import appland.Icons;
import com.intellij.execution.Executor;
import com.intellij.execution.ExecutorRegistry;
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

    public static @NotNull Executor getInstance() {
        var executor = ExecutorRegistry.getInstance().getExecutorById(EXECUTOR_ID);
        assert executor != null;
        return executor;
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
        return Icons.APPMAP_FILE;
    }

    @Override
    public Icon getDisabledIcon() {
        return IconLoader.getDisabledIcon(getIcon());
    }

    @Override
    public @NlsActions.ActionDescription String getDescription() {
        return "AppMap executor";
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
        return AppMapBundle.get("appMapExecutor.startActionConfigName");
    }

    @Override
    public @NotNull String getStartActionText(@NotNull String configurationName) {
        var configName = StringUtil.isEmpty(configurationName) ? "" : " '" + shortenNameIfNeeded(configurationName) + "'";
        return AppMapBundle.get("appMapExecutor.startActionConfigName", configName);
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
