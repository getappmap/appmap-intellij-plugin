package appland.cli;

import com.intellij.execution.process.BaseProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessListener;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Key;
import org.jetbrains.annotations.NotNull;

/**
 * Process adapter which logs events received from the process.
 */
class LoggingProcessAdapter extends ProcessAdapter {
    private static final Logger LOG = Logger.getInstance(LoggingProcessAdapter.class);

    static final ProcessListener INSTANCE = new LoggingProcessAdapter();

    @Override
    public void processTerminated(@NotNull ProcessEvent event) {
        if (LOG.isDebugEnabled()) {
            var process = event.getProcessHandler();
            if (process instanceof BaseProcessHandler<?>) {
                LOG.debug("CLI tool terminated: " + ((BaseProcessHandler<?>) process).getCommandLine() + ", exit code: " + event.getExitCode());
            } else {
                LOG.debug("CLI tool terminated: " + event);
            }
        }
    }

    @Override
    public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(event.getText());
        }
    }
}
