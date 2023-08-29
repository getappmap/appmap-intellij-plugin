package appland.installGuide;

import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.KillableProcessHandler;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessTerminatedListener;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.terminal.TerminalExecutionConsole;
import com.intellij.util.io.BaseDataReader;
import com.intellij.util.io.BaseOutputReader;
import org.jetbrains.annotations.NotNull;

/**
 * RunProfileState, which is suitable for a terminal console with a PTY.
 * The default RunAnythingRunProfileState is not suitable, but we're following it for best practices.
 */
class AppMapRunProfileState extends CommandLineState {
    private final GeneralCommandLine commandLine;

    protected AppMapRunProfileState(@NotNull GeneralCommandLine commandLine, @NotNull ExecutionEnvironment environment) {
        super(environment);
        this.commandLine = commandLine;
    }

    @Override
    public @NotNull ExecutionResult execute(@NotNull Executor executor, @NotNull ProgramRunner<?> runner) throws ExecutionException {
        var processHandler = startProcess();
        ProcessTerminatedListener.attach(processHandler);

        var console = new TerminalExecutionConsole(getEnvironment().getProject(), processHandler);
        console.attachToProcess(processHandler);
        return new DefaultExecutionResult(console, processHandler, createActions(console, processHandler, executor));
    }

    @NotNull
    @Override
    protected ProcessHandler startProcess() throws ExecutionException {
        // not a com.intellij.execution.process.KillableColoredProcessHandler,
        // because the console is handling colors
        return new KillableProcessHandler(commandLine) {
            @Override
            protected BaseOutputReader.@NotNull Options readerOptions() {
                return new BaseOutputReader.Options() {
                    @Override
                    public boolean splitToLines() {
                        return false;
                    }

                    @Override
                    public BaseDataReader.SleepingPolicy policy() {
                        return BaseDataReader.SleepingPolicy.BLOCKING;
                    }
                };
            }
        };
    }
}
