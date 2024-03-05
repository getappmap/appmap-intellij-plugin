package appland.actions;

import appland.AppMapBundle;
import appland.cli.AppLandCommandLineService;
import appland.settings.AppMapProjectSettingsService;
import appland.telemetry.TelemetryService;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.ide.actions.OpenInRightSplitAction;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.UpdateInBackground;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.vcs.changes.ui.VirtualFileListCellRenderer;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.concurrency.annotations.RequiresEdt;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Generates an OpenAPI file using the CLI tool.
 */
public class GenerateOpenApiAction extends AnAction implements DumbAware, UpdateInBackground {
    private static final Logger LOG = Logger.getInstance(GenerateOpenApiAction.class);
    private static final String APPMAP_OPENAPI_FILENAME = "appmap-openapi.yml";

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        createOpenApiFileInteractive(Objects.requireNonNull(e.getProject()), false);
    }

    /**
     * Creates an OpenAPI file with user interaction.
     * If no AppMap directory is available, then an error message will be displayed.
     * If there's exactly one AppMap directory in the project, then no further user interaction happens.
     * If there's more than one AppMap directory in the current project, then a popup will be shown to let the user choose one.
     * <p>
     * After the file was created, the result is shown in a new editor.
     *
     * @param project           The current project
     * @param showInSplitEditor If the result file should be opened in a split editor
     */
    @RequiresEdt
    public static void createOpenApiFileInteractive(@NotNull Project project, boolean showInSplitEditor) {
        var commandLineService = AppLandCommandLineService.getInstance();
        var roots = commandLineService.getActiveRoots();

        if (roots.isEmpty()) {
            Messages.showInfoMessage(project,
                    AppMapBundle.get("action.appmap.generateOpenAPI.noRootMessage.text"),
                    AppMapBundle.get("action.appmap.generateOpenAPI.noRootMessage.title"));
        } else if (roots.size() == 1) {
            createOpenApiFile(project, roots.get(0), showInSplitEditor);
        } else {
            //noinspection RedundantCast,unchecked
            JBPopupFactory.getInstance()
                    .createPopupChooserBuilder(roots)
                    .setRenderer((ListCellRenderer<VirtualFile>) new VirtualFileListCellRenderer(project))
                    .setTitle(AppMapBundle.get("action.appmap.generateOpenAPI.rootChooser.title"))
                    .setMovable(false)
                    .setResizable(false)
                    .setRequestFocus(true)
                    .setItemChosenCallback(root -> createOpenApiFile(project, root, showInSplitEditor))
                    .createPopup()
                    .showInFocusCenter();
        }
    }

    @RequiresEdt
    public static void createOpenApiFile(@NotNull Project project, @NotNull VirtualFile projectRoot, boolean showInSplitEditor) {
        var commandLine = AppLandCommandLineService.getInstance().createGenerateOpenApiCommand(projectRoot);
        if (commandLine == null) {
            LOG.debug("Unable to create command line, e.g. because CLI tool is missing.");
            return;
        }

        // run command in background and show the new result file in a new editor
        new Task.Modal(project, AppMapBundle.get("action.appmap.generateOpenAPI.task.title"), true) {
            private volatile VirtualFile openApiFile;

            @Override
            public void onSuccess() {
                if (openApiFile != null) {
                    var editorManager = FileEditorManager.getInstance(project);
                    if (showInSplitEditor && editorManager.getSelectedEditor() != null) {
                        OpenInRightSplitAction.Companion.openInRightSplit(project, openApiFile, null, true);
                    } else {
                        editorManager.openFile(openApiFile, true);
                    }
                }
            }

            @Override
            public void onFinished() {
                AppMapProjectSettingsService.getState(project).setCreatedOpenAPI(true);
            }

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    var handler = new CapturingProcessHandler(commandLine);
                    var timeout = (int) TimeUnit.SECONDS.toMillis(30);
                    var output = handler.runProcessWithProgressIndicator(indicator, timeout, true);
                    if (output.getExitCode() == 0) {
                        openApiFile = WriteAction.computeAndWait(() -> {
                            try {
                                var file = projectRoot.createChildData(this, APPMAP_OPENAPI_FILENAME);
                                VfsUtil.saveText(file, output.getStdout());
                                return file;
                            } catch (IOException e) {
                                LOG.error(e);
                                return null;
                            }
                        });
                    } else {
                        sendErrorTelemetry();
                    }
                } catch (ExecutionException e) {
                    LOG.debug("Error executing command: " + commandLine.getCommandLineString(), e);
                    sendErrorTelemetry();
                }
            }
        }.queue();
    }

    private static void sendErrorTelemetry() {
        TelemetryService.getInstance().sendEvent("open_api:failure");
    }
}
